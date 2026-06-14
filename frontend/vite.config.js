import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';

export default defineConfig({
  plugins: [react()],
  // lib 모드는 React의 process.env.NODE_ENV를 자동 치환하지 않아 브라우저에서 'process is not defined' 발생.
  // 빌드 시 'production'으로 치환해 process 참조를 제거한다.
  define: {
    'process.env.NODE_ENV': JSON.stringify('production'),
  },
  build: {
    // 백엔드 static/js 로 출력 (경로는 실제 레포 구조에 맞게)
    outDir: path.resolve(import.meta.dirname, '../src/main/resources/static/js'),
    emptyOutDir: false,          // static/js 안 다른 페이지 번들 보존
    lib: {
      entry: {
        'groupbuy-detail': path.resolve(import.meta.dirname, 'src/groupbuy-detail.jsx'),
        'mypage-orders': path.resolve(import.meta.dirname, 'src/mypage-orders.jsx'),
        'mypage-returns': path.resolve(import.meta.dirname, 'src/mypage-returns.jsx'),
      },
      formats: ['es'],           // <script type="module"> 용
      fileName: (format, entryName) => `${entryName}.js`,
    },
  },
});