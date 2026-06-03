// Proxy /api/v1 to the Ktor server during webpack dev-server mode.
// The Kotlin/Wasm dev server typically runs on :8081; Ktor on :8080.
config.devServer = config.devServer || {};
config.devServer.proxy = [
    {
        context: ['/api'],
        target: 'http://localhost:8080',
        changeOrigin: true,
    }
];
