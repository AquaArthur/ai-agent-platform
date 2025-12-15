<template>
  <div id="app">
    <router-view />
  </div>
</template>

<style>
html, body, #app {
  min-height: 100vh;
  height: auto;
  margin: 0;
  padding: 0;
  overflow-x: hidden;
  overflow-y: auto;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
}

#app {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: var(--text-primary);
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
</style>
