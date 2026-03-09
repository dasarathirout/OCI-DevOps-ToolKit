package dasarathi.devops.toolkit.core;

import static dasarathi.devops.toolkit.core.DevOpsToolKitConstants.MESSAGE_TEST_DISABLED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class DevOpsToolKitUtilTest {
  String region = "us-phoenix-1";
  String tenancy = "ocid1.tenancy.oc1.test";
  String fingerPrint = "1A:2B:3C:4D:5E:6F:7G:8I:9L:0L";
  String keyFile = "/Users/dasarathi/.oci/sessions/OCI_DEVOPS_SCM/oci_api_key.pem";
  String securityTokenFile = "/Users/dasarathi/.oci/sessions/OCI_DEVOPS_SCM/token";

  @Test
  @Disabled(MESSAGE_TEST_DISABLED)
  void testLoadConfigProfileTest() {
    String expirationTime = DevOpsToolKitUtil.sessionTokenExpirationTime();
    assertNotNull(expirationTime);
  }

  @Test
  void mockProfileConfig() {
    Map<String, String> configMap = new HashMap<>(5);
    configMap.put("region", region);
    configMap.put("tenancy", tenancy);
    configMap.put("key_file", keyFile);
    configMap.put("fingerprint", fingerPrint);
    configMap.put("security_token_file", securityTokenFile);
    assertNotNull(configMap);
    assertEquals(5, configMap.size());
  }
}
