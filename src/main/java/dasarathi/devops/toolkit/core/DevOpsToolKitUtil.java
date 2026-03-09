package dasarathi.devops.toolkit.core;

import static dasarathi.devops.toolkit.core.DevOpsToolKitConstants.DEVOPS_TOOLKIT;
import static dasarathi.devops.toolkit.core.DevOpsToolKitConstants.PROFILE_OCI_DEVOPS_SCM;

import com.intellij.openapi.diagnostic.Logger;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.SessionTokenAuthenticationDetailsProvider;
import com.oracle.bmc.devops.DevopsAsyncClient;
import com.oracle.bmc.devops.DevopsClient;
import dasarathi.devops.toolkit.exception.DevOpsPluginException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public final class DevOpsToolKitUtil {

  private static final Logger LOG = Logger.getInstance(DevOpsToolKitUtil.class);

  private static final String K1_FINGERPRINT = "fingerprint";
  private static final String K2_KEY_FILE = "key_file";
  private static final String K3_TENANCY = "tenancy";
  private static final String K4_REGION = "region";
  private static final String K5_SECURITY_TOKEN_FILE = "security_token_file";

  private record ConfigProfile(
      String fingerprint,
      String keyfile,
      String tenancy,
      String region,
      String securityTokenFile) {}

  private DevOpsToolKitUtil() {
    /*NOOP*/
  }

  public static DevopsClient getDevOpsClient() {
    return new DevopsClient(getSessionTokenProvider());
  }

  public static DevopsAsyncClient getDevOpsAsyncClient() {
    return new DevopsAsyncClient(getSessionTokenProvider());
  }

  public static String getRequestId(final String apiReferenceEndpoint) {
    return String.format("%s-%s-%s", DEVOPS_TOOLKIT, apiReferenceEndpoint, UUID.randomUUID());
  }

  private static ConfigProfile getConfigProfile() {
    try {
      ConfigFileReader.ConfigFile configFile =
          ConfigFileReader.parseDefault(PROFILE_OCI_DEVOPS_SCM);
      ConfigProfile ociConfigProfile =
          new ConfigProfile(
              configFile.get(K1_FINGERPRINT),
              configFile.get(K2_KEY_FILE),
              configFile.get(K3_TENANCY),
              configFile.get(K4_REGION),
              configFile.get(K5_SECURITY_TOKEN_FILE));
      LOG.info(String.format("OCI config profile: %s", ociConfigProfile));
      return ociConfigProfile;
    } catch (Exception exception) {
      throw new DevOpsPluginException(
          "OCI profile not found: " + PROFILE_OCI_DEVOPS_SCM, exception);
    }
  }

  private static Path resolveSecurityTokenPath(ConfigProfile configProfile) {
    String securityTokenFileValue = configProfile.securityTokenFile();
    if (securityTokenFileValue == null || securityTokenFileValue.isBlank()) {
      throw new DevOpsPluginException(
          "Security token file is not configured for profile: " + PROFILE_OCI_DEVOPS_SCM);
    }
    Path securityTokenFile = Path.of(securityTokenFileValue).toAbsolutePath().normalize();
    if (!Files.isReadable(securityTokenFile)) {
      LOG.warn(String.format("Security token file is not readable: %s", securityTokenFile));
      throw new DevOpsPluginException("Security token file is not readable: " + securityTokenFile);
    }
    return securityTokenFile;
  }

  private static String sessionTokenExpirationTime(ConfigProfile configProfile) {
    Path securityTokenFile = resolveSecurityTokenPath(configProfile);
    try {
      String securityTokenContent = Files.readString(securityTokenFile);
      JWTClaimsSet claims = SignedJWT.parse(securityTokenContent).getJWTClaimsSet();
      return String.valueOf(claims.getExpirationTime());
    } catch (Exception e) {
      throw new DevOpsPluginException("Failed to read security token expiration", e);
    }
  }

  public static String sessionTokenExpirationTime() {
    return sessionTokenExpirationTime(getConfigProfile());
  }

  public static Date sessionTokenExpirationDate() {
    try {
      ConfigProfile configProfile = getConfigProfile();
      Path securityTokenFile = resolveSecurityTokenPath(configProfile);
      String securityTokenContent = Files.readString(securityTokenFile);
      JWTClaimsSet claims = SignedJWT.parse(securityTokenContent).getJWTClaimsSet();
      return claims.getExpirationTime();
    } catch (Exception e) {
      throw new DevOpsPluginException("Failed to read security token expiration", e);
    }
  }

  public static String sessionTokenExpirationTimeString() {
    return new SimpleDateFormat("MM/dd/yy h:mm a").format(sessionTokenExpirationDate());
  }

  public static SessionTokenAuthenticationDetailsProvider getSessionTokenProvider() {
    try {
      ConfigFileReader.ConfigFile configFile =
          ConfigFileReader.parseDefault(PROFILE_OCI_DEVOPS_SCM);
      return new SessionTokenAuthenticationDetailsProvider(configFile);
    } catch (final Exception e) {
      throw new DevOpsPluginException("Config Session Token Provider Having Issue:", e);
    }
  }
}
