function workflow() {
  return {
    // Tabs: 'create' = anlegen, 'execute' = ausführen
    tab: 'execute',
    theme: 'dark',
    createMode: '',            // 'new', 'step', 'step-edit', 'create' sein
    selectedDeploymentId: '',  // für Step hinzufügen
    stepId: null,

    // Daten
    repos: [],
    repoId: null,
    playbooks: [],
    inventories: [],
    deployments: [],

    // Auswahl beim Anlegen
    selectedPlaybooks: [],
    selectedInventory: '',
    deploymentName: '',
    tags: '',
    skipTags: '',
    hostLimit: '',

    // Outputs für Runs
    outputs: [],

    // Become-Passwort Dialog
    showBecomePrompt: false,
    becomePassword: '',
    pendingRunId: null,

    init() {
      this.loadRepos();
      this.loadDeployments();
    },

    getCsrfToken() {
      const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
      return match ? decodeURIComponent(match[1]) : '';
    },

    logout() {
      fetch('/logout', {
        method: 'POST',
        headers: { 'X-XSRF-TOKEN': this.getCsrfToken() }
      }).then(() => { window.location.href = '/login?logout'; });
    },

    toggleTheme() {
      this.theme = this.theme === 'dark' ? 'light' : 'dark';
    },

    joinDistinct(arr) {
      const vals = (arr || []).filter(Boolean);
      return [...new Set(
        vals.flatMap(v => ('' + v).split(',')
          .map(s => s.trim())
          .filter(Boolean))
      )].join(', ');
    },

    canCreateDeployment() {
      return this.repoId && this.deploymentName.trim().length > 0;
    },

    resetDeploymentForm() {
      const currentRepo = this.repoId;

      this.deploymentName = "";
      this.selectedPlaybooks = [];
      this.selectedInventory = "";
      this.tags = "";
      this.skipTags = "";
      this.hostLimit = "";
      this.selectedDeploymentId = "";
      this.createMode = "";

      this.repoId = currentRepo;
      if (currentRepo) {
        this.selectRepo(currentRepo);
      }
    },

    addStepToDeployment(repoId) {
      this.repoId = repoId;
      if (!this.selectedDeploymentId) return;
      const promises = this.selectedPlaybooks.map(p =>
        fetch(`/api/${this.repoId}/deployment/${this.selectedDeploymentId}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-XSRF-TOKEN': this.getCsrfToken() },
          body: `playbook=${encodeURIComponent(p)}`
            + `&inventory=${encodeURIComponent(this.selectedInventory)}`
            + `&tags=${encodeURIComponent(this.tags)}`
            + `&skipTags=${encodeURIComponent(this.skipTags)}`
            + `&hostLimit=${encodeURIComponent(this.hostLimit)}`
        })
      );
      Promise.all(promises).then(() => {
        this.loadDeployments();
        this.tab = 'execute'; // nach Hinzufügen direkt zur Ausführung wechseln
      });
    },

    loadRepos() {
      fetch('/repos')
        .then(r => r.json())
        .then(data => { this.repos = data; })
        .catch(err => console.error('Fehler beim Laden der Repos:', err));
    },

    selectRepo(id) {
      this.repoId = id;

      fetch(`/api/${id}/inventories`)
        .then(r => r.json())
        .then(data => { this.inventories = data; });

      if (this.createMode === 'step') {
        fetch(`/api/${id}/playbooks`)
          .then(r => r.json())
          .then(data => { this.playbooks = data; });
      } else {
        this.playbooks = [];
        this.selectedPlaybooks = [];
      }

      this.loadDeployments();
    },

    // Deployments laden
    loadDeployments() {
      if (!this.repoId) {
        this.deployments = [];
        return;
      }
      fetch(`/api/${this.repoId}/deployments`)
        .then(r => r.json())
        .then(data => { this.deployments = data; })
        .catch(err => console.error('Fehler beim Laden der Deployments:', err));
    },

    // Deployment anlegen
    async createDeployment() {
      if (!this.canCreateDeployment()) return;
      const r = await fetch(`/api/${this.repoId}/deployment`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-XSRF-TOKEN': this.getCsrfToken() },
        body: `name=${encodeURIComponent(this.deploymentName)}`
      });
      const d = await r.json();
      await Promise.all(this.selectedPlaybooks.map(p =>
        fetch(`/api/${this.repoId}/deployment/${d.id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-XSRF-TOKEN': this.getCsrfToken() },
          body: `playbook=${encodeURIComponent(p)}`
            + `&inventory=${encodeURIComponent(this.selectedInventory)}`
            + `&tags=${encodeURIComponent(this.tags)}`
            + `&skipTags=${encodeURIComponent(this.skipTags)}`
            + `&hostLimit=${encodeURIComponent(this.hostLimit)}`
        })
      ));
      this.loadDeployments();
      this.tab = 'execute';
    },

    addStep(deploymentId, repoId) {
      this.createMode = 'step-add';
      this.tab = 'create';
      this.stepId = null;
      this.selectedDeploymentId = deploymentId;
      this.selectedPlaybooks = [];
      this.selectedInventory = '';
      this.tags = '';
      this.skipTags = '';
      this.hostLimit = '';
      this.repoId = repoId;

      Promise.all([
        fetch(`/api/${repoId}/playbooks`).then(r => r.json()),
        fetch(`/api/${repoId}/inventories`).then(r => r.json())
      ]).then(([playbooks, inventories]) => {
        this.playbooks = playbooks;
        this.inventories = inventories;
      });
    },

    runDeployment(id) {
      fetch(`/api/${this.repoId}/deployment/${id}/requires-become`)
        .then(r => r.json())
        .then(data => {
          if (data.requiresBecome) {
            this.pendingRunId = id;
            this.showBecomePrompt = true;
          } else {
            this.startRun(id, null);
          }
        })
        .catch(() => this.startRun(id, null));
    },

    confirmRun() {
      this.showBecomePrompt = false;
      this.startRun(this.pendingRunId, this.becomePassword);
      this.becomePassword = '';
      this.pendingRunId = null;
    },

    startRun(id, becomePassword) {
      const outputObj = { id: Date.now(), deploymentId: id, text: '' };
      this.outputs.push(outputObj);

      const body = new URLSearchParams();
      if (becomePassword) body.append('becomePassword', becomePassword);

      fetch(`/api/${this.repoId}/rundeployment/${id}`, {
        method: 'POST',
        headers: { 'X-XSRF-TOKEN': this.getCsrfToken() },
        body
      }).then(res => {
        const reader = res.body.getReader();
        const decoder = new TextDecoder();

        const pump = () => reader.read().then(({ done, value }) => {
          if (done) return;
          outputObj.text += decoder.decode(value);
          this.outputs = this.outputs.map(o => o.id === outputObj.id ? { ...outputObj } : o);
          return pump();
        });

        return pump();
      });
    },

    editStep(deploymentId, step, repoId) {
      this.createMode = 'step-edit';
      this.tab = 'create';
      this.selectedDeploymentId = deploymentId;
      this.stepId = step.id;
      this.tags = step.tags || '';
      this.skipTags = step.skipTags || '';
      this.hostLimit = step.hostLimit || '';
      this.repoId = repoId;

      Promise.all([
        fetch(`/api/${repoId}/playbooks`).then(r => r.json()),
        fetch(`/api/${repoId}/inventories`).then(r => r.json())
      ]).then(([playbooks, inventories]) => {
        this.playbooks = playbooks;
        this.inventories = inventories;
        this.selectedPlaybooks = [step.playbook];
        this.selectedInventory = step.inventory;
      });
    },

    updateStep() {
      const body = {
        playbook: this.selectedPlaybooks[0],
        inventory: this.selectedInventory,
        tags: this.tags,
        skipTags: this.skipTags,
        hostLimit: this.hostLimit
      };

      fetch(`/api/${this.repoId}/deployment/${this.selectedDeploymentId}/step/${this.stepId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': this.getCsrfToken() },
        body: JSON.stringify(body)
      }).then(() => {
        this.loadDeployments();
        this.createMode = '';
        this.tab = 'execute';
      });
    },

    deleteDeployment(id) {
      if (!confirm("Deployment wirklich löschen?")) return;
      fetch(`/api/${this.repoId}/deployment/${id}`, { method: 'DELETE', headers: { 'X-XSRF-TOKEN': this.getCsrfToken() } })
        .then(() => this.loadDeployments())
        .catch(err => console.error('Fehler beim Löschen des Deployments:', err));
    },

    deleteStep(deploymentId, stepId) {
      if (!confirm("Step wirklich löschen?")) return;
      fetch(`/api/${this.repoId}/deployment/${deploymentId}/step/${stepId}`, {
        method: 'DELETE',
        headers: { 'X-XSRF-TOKEN': this.getCsrfToken() }
      }).then(() => {
        this.loadDeployments();
        this.createMode = '';
        this.tab = 'execute';
      }).catch(err => console.error('Fehler beim Löschen des Steps:', err));
    },

    copySingleOutput(o) {
      navigator.clipboard.writeText(o.text || '').then(() => {
        alert(`Output von Deployment ${o.deploymentId} kopiert!`);
      });
    },

    deleteSingleOutput(id) {
      this.outputs = this.outputs.filter(o => o.id !== id);
    },

  };
}
