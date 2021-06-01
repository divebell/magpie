package io.openraven.magpie.plugins.aws.discovery.services;

import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieEnvelope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iam.IamClient;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class IAMCredentialReportDiscoveryIT extends BaseAWSServiceIT {

  private static final String TEST_USER = "testusername";
  private static final IAMDiscovery iamDiscovery = new IAMDiscovery();

  @Mock
  private Emitter emitter;

  @Captor
  private ArgumentCaptor<MagpieEnvelope> envelopeCapture;

  @Test
  public void testCredentialReportDiscovery() {
    //
    IamClient iamClient = IamClient.builder()
      .endpointOverride(URI.create(System.getProperty("MAGPIE_AWS_ENDPOINT")))
      .region(BASE_REGION)
      .build();
    // given
    createIAMUser(iamClient, TEST_USER);
    // when
    iamDiscovery.discoverCredentialsReport(
      iamClient,
      MAPPER,
      SESSION,
      BASE_REGION,
      emitter,
      LOGGER,
      ACCOUNT
    );
    // then
    Mockito.verify(emitter).emit(envelopeCapture.capture());
    var contents = envelopeCapture.getValue().getContents();

    assertNotNull(contents.get("documentId"));
    assertEquals(String.format("arn:aws:iam::000000000000:user/%s", TEST_USER), contents.get("arn").asText());
    assertEquals(TEST_USER, contents.get("resourceName").asText());
    assertEquals(String.format("arn:aws:iam::000000000000:user/%s", TEST_USER), contents.get("resourceId").asText());
    assertEquals("AWS::IAM::CredentialsReport", contents.get("resourceType").asText());
    assertEquals(ACCOUNT, contents.get("awsAccountId").asText());
    assertEquals(BASE_REGION.toString(), contents.get("awsRegion").asText());
  }


  private void createIAMUser(IamClient iamClient, String username ) {
    iamClient.createUser(req -> req.userName(username));
  }
}
