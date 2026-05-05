// Middleware to verify requests coming from the Minecraft plugin
function pluginAuth(req, res, next) {
  const secret = req.headers['x-api-secret'];
  if (!secret || secret !== process.env.API_SECRET) {
    return res.status(401).json({ error: 'Unauthorized' });
  }
  next();
}

module.exports = { pluginAuth };
