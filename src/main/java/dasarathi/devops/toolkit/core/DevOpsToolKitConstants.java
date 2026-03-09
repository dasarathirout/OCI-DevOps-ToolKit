package dasarathi.devops.toolkit.core;

public final class DevOpsToolKitConstants {
  private DevOpsToolKitConstants() {}

  /*
   * DEV
   * ===========================================================================
   * */
  public static final String OCI_CONSOLE_URL = "https://cloud.oracle.com";
  public static final String SUPPRESS_ALL_WARNING = "all";

  /*
   * UTILs
   * ===========================================================================
   * */
  public static final String DEVOPS_TOOLKIT = "DevOpsTookKit";
  public static final String DEVOPS_TOOLKIT_WINDOW = "OCI-DevOps-ToolKit-ToolWindow";
  public static final String DEVOPS_PLUGIN_ID = "dasarathi.devops.toolkit-intellij-plugin";
  public static final String GROUP_DEVOPS_TOOLKIT_NOTIFICATION = "DevopsToolKitEventNotification";
  public static final String TITLE_NOTIFICATION_PROCESS_FAILED = "Process Failed";
  public static final String TITLE_NOTIFICATION_INFO = "OCI DevOps Toolkit";

  /*
   * SESSION
   * ===========================================================================
   * */
  public static final String REGION_US_PHOENIX_1 = "us-phoenix-1";
  public static final String TENANCY_BMC_OPERATOR_ACCESS = "bmc_operator_access";
  public static final String PROFILE_OCI_DEVOPS_SCM = "OCI_DEVOPS_SCM";
  public static final String IDENTITY_PROVIDER_OCNA_SAML = "ocna-saml";

  /*
   * OCI OCIDs
   * ===========================================================================
   * */
  public static final String DEVOPS_SCM_URL =
      "https://oci.private.devops.scmservice.us-phoenix-1.oci.oracleiaas.com";
  public static final String DEVOPS_COMPARTMENT_ID =
      "ocid1.compartment.oc1..aaaaaaaafs2uigzlcc6h6hx5gc6pm2qdn7bxgjt22llyivlktoc6gbdvqxva";
  public static final String DEVOPS_PROJECT_ID =
      "ocid1.devopsproject.oc1.phx.amaaaaaaw4vcxbyajigjxw7wlnhgturi5657fi6yi2m3u44s3gvxwyy7zpfa";

  /*
   * TEST
   * ===========================================================================
   * */
  public static final String MESSAGE_TEST_DISABLED =
      "Current TestCase Disabled For Gradle Build. Can Run Locally";
}
