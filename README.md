# Deployer

Spring Boot Projekt zur Verwaltung und Ausführung von Ansible-Playbooks in lokalen Repositories.

## Übersicht
Die Anwendung startet via
```bash
mvn spring-boot:run
```

und bietet:
- Repo-Verwaltung (Konfiguration: [application.yaml](application.yaml)
- Git-Operationen: [`com.example.deployer.frontend.GitController`](src/main/java/com/example/deployer/frontend/GitController.java)
- Auflisten von Repositories: [`com.example.deployer.frontend.RepoController`](src/main/java/com/example/deployer/frontend/RepoController.java)
- Deployment-Workflow und Streaming-Ausgabe: [`com.example.deployer.frontend.DeploymentController`](src/main/java/com/example/deployer/frontend/DeploymentController.java)
- Playbook-Ausführung: [`com.example.deployer.actions.PlaybookService`](src/main/java/com/example/deployer/actions/PlaybookService.java)

Konfiguration der Repos erfolgt in [application.yaml](application.yaml) unter dem Prefix `git` und wird durch [`com.example.deployer.configuration.GitConfig`](src/main/java/com/example/deployer/configuration/GitConfig.java) geladen.

## Voraussetzungen
- Java 21
- Maven (Wrapper im Projekt vorhanden) — siehe [pom.xml](pom.xml)
- Ansible auf dem Host, wenn Playbooks ausgeführt werden sollen

## Build & Start
Projekt bauen und starten:
```bash
mvn clean package
java -jar target/deployer-0.0.1-SNAPSHOT.jar
```

## Ablauf im Überblick
- Repos anzeigen → /repos
- Playbooks anzeigen → /api/{repoId}/playbooks
- Deployment anlegen → /api/{repoId}/deployment
- Playbook hinzufügen → /api/{repoId}/deployment/{id}
- Deployment starten → /api/{repoId}/rundeployment/{id}

## Komplettes Beispiel: Deployment mit curl
### Repos anzeigen
Zuerst alle konfigurierten Repos abrufen:
```bash
curl -X GET "http://localhost:8080/repos"
```

Beispielantwort:
```json
[
  {
    "id": "repo1",
    "path": "../repo1",
    "playbooksDir": "playbooks",
    "inventoriesDir": "inventory"
  },
  {
    "id": "repo2",
    "path": "../repo2",
    "playbooksDir": "ansible/playbooks",
    "inventoriesDir": "ansible/inventories"
  }
]
```

### Pybooks eines Repos anzeigen
Nun die Playbooks von repo1 auflisten:

```bash
curl -X GET "http://localhost:8080/api/repo1/playbooks"
```
Beispielantwort:

```json
["apt_update", "mariadbcluster", "k3s"]
```

### Neues Deployment anlegen
Ein Deployment für repo1 erstellen:

```bash
curl -X POST "http://localhost:8080/api/repo1/deployment" \
     -d "name=MeinDeployment"
```
Beispielantwort:
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "MeinDeployment",
  "repoId": "repo1"
}
``` 

### Playbook zum Deployment hinzufügen
Ein Playbook (site) mit einem Inventory (dev) hinzufügen:

```bash
curl -X PUT "http://localhost:8080/api/repo1/deployment/123e4567-e89b-12d3-a456-426614174000" \
     -d "playbook=apt_update" \
     -d "inventory=dev" \
     -d "tags=setup" \
     -d "skipTags=tests"
```
Beispielantwort:
```json
{
  "status": "added",
  "deploymentId": "123e4567-e89b-12d3-a456-426614174000",
  "repoId": "repo1"
}
```
### Deployment ausführen
Das Deployment starten und die Ausgabe streamen:

```bash
curl -X GET "http://localhost:8080/api/repo1/rundeployment/123e4567-e89b-12d3-a456-426614174000"
👉 Ausgabe: Live‑Stream der Ansible‑Playbook‑Ausführung (z. B. TASK [setup] ... ok).
```
