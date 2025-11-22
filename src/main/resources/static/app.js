function workflow() {
  return {
    // Tabs: 'create' = anlegen, 'execute' = ausführen
    tab: 'create',
    theme: 'dark',

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

    // Outputs für Runs
    outputs: [],

    // Init: Repos und Deployments sofort laden
    init() {
      this.loadRepos();
      this.repoId = "repo1"; // Standard-Repo oder global
      this.loadDeployments();
    },

    // Theme toggle
    toggleTheme() {
      this.theme = this.theme === 'dark' ? 'light' : 'dark';
    },

    // Hilfsfunktionen
    joinDistinct(arr) {
      const vals = (arr || []).filter(Boolean);
      return [...new Set(
        vals.flatMap(v => ('' + v).split(',')
          .map(s => s.trim())
          .filter(Boolean))
      )].join(', ');
    },

    canCreateDeployment() {
      return this.repoId &&
        this.deploymentName.trim().length > 0 &&
        this.selectedPlaybooks.length > 0 &&
        this.selectedInventory;
    },

    // Repos laden
    loadRepos() {
      fetch('/repos')
        .then(r => r.json())
        .then(data => { this.repos = data; });
    },

    // Repo auswählen
    selectRepo(id) {
      this.repoId = id;
      fetch(`/api/${id}/playbooks`)
        .then(r => r.json())
        .then(data => { this.playbooks = data; });
      fetch(`/api/${id}/inventories`)
        .then(r => r.json())
        .then(data => { this.inventories = data; });
      this.loadDeployments();
    },

    // Deployments laden
    loadDeployments() {
      if (!this.repoId) return;
      fetch(`/api/${this.repoId}/deployments`)
        .then(r => r.json())
        .then(data => { this.deployments = data; });
    },

    // Deployment anlegen
    createDeployment() {
      if (!this.canCreateDeployment()) return;
      fetch(`/api/${this.repoId}/deployment`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: `name=${encodeURIComponent(this.deploymentName)}`
      })
        .then(r => r.json())
        .then(d => {
          const promises = this.selectedPlaybooks.map(p =>
            fetch(`/api/${this.repoId}/deployment/${d.id}`, {
              method: 'PUT',
              headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
              body: `playbook=${encodeURIComponent(p)}&inventory=${encodeURIComponent(this.selectedInventory)}&tags=${encodeURIComponent(this.tags)}&skipTags=${encodeURIComponent(this.skipTags)}`
            })
          );
          return Promise.all(promises).then(() => d);
        })
        .then(() => {
          this.loadDeployments();
          this.tab = 'execute'; // nach dem Anlegen direkt zur Ausführung wechseln
        });
    },

    // Deployment starten → Output-Block erzeugen
    runDeployment(id) {
      const outputObj = { id: Date.now(), deploymentId: id, text: '' };
      this.outputs.push(outputObj);

      fetch(`/api/${this.repoId}/rundeployment/${id}`).then(res => {
        const reader = res.body.getReader();
        const decoder = new TextDecoder();

        const pump = () => reader.read().then(({ done, value }) => {
          if (done) return;
          // Text anhängen
          outputObj.text += decoder.decode(value);
          // Reaktivität erzwingen: Array neu zuweisen
          this.outputs = this.outputs.map(o => o.id === outputObj.id ? { ...outputObj } : o);
          return pump();
        });

        return pump();
      });
    },

    // Deployment löschen
    deleteDeployment(id) {
      if (!confirm("Deployment wirklich löschen?")) return;
      fetch(`/api/${this.repoId}/deployment/${id}`, { method: 'DELETE' })
        .then(() => this.loadDeployments());
    },

    // Output Aktionen
    copyOutput() {
      const allText = this.outputs.map(o => o.text).join("\n\n");
      navigator.clipboard.writeText(allText || '').then(() => {
        alert('Alle Ergebnisse kopiert!');
      });
    },
    clearOutput() {
      this.outputs = [];
    }
  };
}
