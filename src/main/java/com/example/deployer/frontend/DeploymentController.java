package com.example.deployer.frontend;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import com.example.deployer.actions.PlaybookService;
import com.example.deployer.configuration.GitConfig;
import com.example.deployer.entities.Deployment;
import com.example.deployer.entities.DeploymentItem;
import com.example.deployer.entities.DeploymentItemRepository;
import com.example.deployer.entities.DeploymentRepository;

@RestController
@RequestMapping("/api/{repoId}")
public class DeploymentController {

    private final DeploymentRepository deploymentRepo;
    private final DeploymentItemRepository itemRepo;
    private final PlaybookService playbookService;
    private final GitConfig gitConfig;

    public DeploymentController(DeploymentRepository deploymentRepo,
            DeploymentItemRepository itemRepo,
            PlaybookService playbookService,
            GitConfig gitConfig) {
        this.deploymentRepo = deploymentRepo;
        this.itemRepo = itemRepo;
        this.playbookService = playbookService;
        this.gitConfig = gitConfig;
    }

    @GetMapping("/playbooks")
    public List<String> listPlaybooks(@PathVariable String repoId) {
        GitConfig.Repo repo = gitConfig.findById(repoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repo not found"));

        File playbooksDir = new File(repo.getPath(), repo.getPlaybooksDir());
        if (!playbooksDir.exists() || !playbooksDir.isDirectory()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Kein gültiger Playbooks-Ordner im Repo gefunden");
        }

        File[] files = playbooksDir.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files == null)
            return List.of();

        return Arrays.stream(files)
                .filter(File::isFile)
                .map(File::getName)
                .map(name -> name.replaceAll("\\.ya?ml$", "")) // Endung entfernen
                .sorted()
                .toList();
    }

    @GetMapping("/inventories")
    public List<String> listInventories(@PathVariable String repoId) {
        GitConfig.Repo repo = gitConfig.findById(repoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repo not found"));

        File inventoriesDir = new File(repo.getPath(), repo.getInventoriesDir());
        if (!inventoriesDir.exists() || !inventoriesDir.isDirectory()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Kein gültiger Inventories-Ordner im Repo gefunden");
        }

        File[] files = inventoriesDir.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files == null)
            return List.of();

        return Arrays.stream(files)
                .filter(File::isFile)
                .map(File::getName)
                .map(name -> name.replaceAll("\\.ya?ml$", "")) // Endung entfernen
                .sorted()
                .toList();
    }

    @PostMapping("/deployment")
    public Deployment createDeployment(@PathVariable String repoId, @RequestParam String name) {
        Deployment d = new Deployment(UUID.randomUUID().toString(), name, repoId);
        return deploymentRepo.save(d);
    }

    @GetMapping("/deployment/{id}")
    public Map<String, Object> getDeployment(@PathVariable String repoId, @PathVariable String id) {
        Deployment d = deploymentRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deployment not found"));
        if (!d.getRepoId().equals(repoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deployment gehört zu anderem Repo");
        }
        List<DeploymentItem> items = itemRepo.findByDeploymentId(id);
        return Map.of("id", d.getId(), "name", d.getName(), "repoId", d.getRepoId(), "items", items);
    }

    @GetMapping("/deployments")
    public List<Map<String, Object>> listDeployments(@PathVariable String repoId) {
        List<Deployment> deployments = deploymentRepo.findByRepoId(repoId);
        return deployments.stream().map(d -> {
            List<DeploymentItem> items = itemRepo.findByDeploymentId(d.getId());
            return Map.of(
                    "id", d.getId(),
                    "name", d.getName(),
                    "repoId", d.getRepoId(),
                    "items", items.stream().map(i -> Map.of(
                            "playbook", i.getPlaybook(),
                            "inventory", i.getInventory(),
                            "tags", i.getTags(),
                            "skipTags", i.getSkipTags())).toList());
        }).toList();
    }

    @PutMapping("/deployment/{id}")
    public Map<String, String> addPlaybook(@PathVariable String repoId, @PathVariable String id,
            @RequestParam String playbook,
            @RequestParam String inventory,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String skipTags) {
        Deployment d = deploymentRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deployment not found"));
        if (!d.getRepoId().equals(repoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deployment gehört zu anderem Repo");
        }
        DeploymentItem item = new DeploymentItem(null, id, repoId, playbook, inventory, tags, skipTags);
        itemRepo.save(item);
        return Map.of("status", "added", "deploymentId", id, "repoId", repoId);
    }

    @DeleteMapping("/deployment/{id}")
    public ResponseEntity<Void> deleteDeployment(@PathVariable String repoId, @PathVariable String id) {
        Deployment d = deploymentRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deployment not found"));

        if (!d.getRepoId().equals(repoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deployment gehört zu anderem Repo");
        }

        // Zuerst alle Items löschen
        List<DeploymentItem> items = itemRepo.findByDeploymentId(id);
        itemRepo.deleteAll(items);

        // Dann das Deployment selbst löschen
        deploymentRepo.delete(d);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rundeployment/{id}")
    public ResponseBodyEmitter runDeployment(@PathVariable String repoId, @PathVariable String id) {
        Deployment d = deploymentRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deployment not found"));
        if (!d.getRepoId().equals(repoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deployment gehört zu anderem Repo");
        }

        List<DeploymentItem> items = itemRepo.findByDeploymentId(id);
        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Deployment has no playbooks");
        }

        GitConfig.Repo repo = gitConfig.findById(repoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repo not found"));

        ResponseBodyEmitter emitter = new ResponseBodyEmitter();
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                for (DeploymentItem item : items) {
                    // Playbook-Datei suchen (.yml/.yaml)
                    File playbookFile = resolveFileWithYamlExtensions(
                            new File(repo.getPath(), repo.getPlaybooksDir()), item.getPlaybook());
                    if (!playbookFile.exists()) {
                        emitter.send("❌ Playbook nicht gefunden: " + item.getPlaybook() + "\n");
                        continue;
                    }

                    // Inventory-Datei suchen (.yml/.yaml)
                    File inventoryFile = resolveFileWithYamlExtensions(
                            new File(repo.getPath(), repo.getInventoriesDir()), item.getInventory());
                    if (!inventoryFile.exists()) {
                        emitter.send("❌ Inventory nicht gefunden: " + item.getInventory() + "\n");
                        continue;
                    }

                    playbookService.runPlaybookStreamed(
                            playbookFile.getAbsolutePath(),
                            inventoryFile.getAbsolutePath(),
                            emitter);
                }
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    private File resolveFileWithYamlExtensions(File baseDir, String nameWithoutExt) {
        File yml = new File(baseDir, nameWithoutExt + ".yml");
        File yaml = new File(baseDir, nameWithoutExt + ".yaml");
        if (yml.exists())
            return yml;
        if (yaml.exists())
            return yaml;
        return yml;
    }

}
