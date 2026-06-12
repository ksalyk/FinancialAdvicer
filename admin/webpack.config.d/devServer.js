// Proxy /api/v1 to the Ktor server during webpack dev-server mode.
// Ktor listens on :8082 (see server Application.kt — PORT env, default 8082).
config.devServer = config.devServer || {};
config.devServer.proxy = [
    {
        context: ['/api'],
        target: 'http://localhost:8082',
        changeOrigin: true,
    }
];
