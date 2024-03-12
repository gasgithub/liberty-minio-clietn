FROM icr.io/appcafe/open-liberty:24.0.0.2-kernel-slim-java11-openj9-ubi

# Add Liberty server configuration including all necessary features
COPY --chown=1001:0  src/main/liberty/config/server.xml /config/

# This script will add the requested XML snippets to enable Liberty features and grow image to be fit-for-purpose using featureUtility.
# Only available in 'kernel-slim'. The 'full' tag already includes all features for convenience.
RUN features.sh

# Add app
COPY --chown=1001:0  target/mino-rest-client.war /config/apps/

# This script will add the requested server configurations, apply any interim fixes and populate caches to optimize runtime
RUN configure.sh