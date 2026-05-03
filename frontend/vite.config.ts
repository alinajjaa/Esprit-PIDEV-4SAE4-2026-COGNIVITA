import { defineConfig } from 'vite';

export default defineConfig({
  assetsInclude: [
    '**/*-shard1',
    '**/*-shard2',
    '**/*-shard3',
  ]
});