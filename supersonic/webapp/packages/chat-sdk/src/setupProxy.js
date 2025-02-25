const {createProxyMiddleware} = require('http-proxy-middleware');

module.exports = function (app) {
    app.use(
        '/supersonic/api/',
        createProxyMiddleware({
            target: 'http://localhost:9080',
            changeOrigin: true,
        })
    );
    app.use(
        '/supersonic/openapi',
        createProxyMiddleware({
            target: 'http://localhost:9080',
            changeOrigin: true,
        })
    );
};
