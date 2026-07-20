<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  -webkit-overflow-scrolling: touch;
}
#app {
  -webkit-tap-highlight-color: transparent;
}
:deep(.van-picker) {
  touch-action: pan-y;
}
:deep(.van-calendar) {
  touch-action: pan-y;
}
:deep(.van-calendar__days) {
  touch-action: pan-y;
}
</style>