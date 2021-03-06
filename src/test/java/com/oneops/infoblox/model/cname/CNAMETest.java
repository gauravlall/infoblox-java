package com.oneops.infoblox.model.cname;

import static com.oneops.infoblox.IBAEnvConfig.domain;
import static com.oneops.infoblox.IBAEnvConfig.isValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.oneops.infoblox.IBAEnvConfig;
import com.oneops.infoblox.InfobloxClient;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Canonical record tests.
 *
 * @author Suresh G
 */
@DisplayName("Infoblox CNAME record tests.")
class CNAMETest {

  private static InfobloxClient client;

  @BeforeAll
  static void setUp() {
    assumeTrue(isValid(), IBAEnvConfig::errMsg);
    client =
        InfobloxClient.builder()
            .endPoint(IBAEnvConfig.host())
            .userName(IBAEnvConfig.user())
            .password(IBAEnvConfig.password())
            .ttl(1)
            .tlsVerify(false)
            .debug(true)
            .build();
  }

  @ParameterizedTest
  @ValueSource(strings = {"oneops-test"})
  void create(String prefix) throws IOException {

    final String canonicalName = String.format("%s.%s", prefix, domain());
    final String newCanonicalName = String.format("%s-mod.%s", prefix, domain());
    final String alias = String.format("%s-cname1.%s", prefix, domain());
    final String newAlias = String.format("%s-cname1-mod.%s", prefix, domain());

    // Clean it.
    client.deleteCNameRec(alias);
    client.deleteCNameRec(newAlias);

    List<CNAME> rec = client.getCNameRec(alias);
    assertTrue(rec.isEmpty());

    // Creates CNAME Record
    CNAME cname = client.createCNameRec(alias, canonicalName);
    assertEquals(canonicalName, cname.canonical());

    List<CNAME> cNameRec = client.getCNameRec(alias);
    assertEquals(1, cNameRec.size());
    assertEquals(canonicalName, cNameRec.get(0).canonical());

    // Modify CNAME Record
    List<CNAME> modCName = client.modifyCNameRec(alias, newAlias);
    assertEquals(1, modCName.size());

    // Modify canonical record
    List<CNAME> modCName1 = client.modifyCNameCanonicalRec(newAlias, newCanonicalName);
    assertEquals(1, modCName1.size());
    assertEquals(newAlias, modCName1.get(0).name());
    assertEquals(newCanonicalName, modCName1.get(0).canonical());

    // Delete CNAME Record
    List<String> delCName = client.deleteCNameRec(alias);
    assertEquals(0, delCName.size());

    List<String> delNewCName = client.deleteCNameRec(newAlias);
    assertEquals(1, delNewCName.size());
  }

  @Test
  @DisplayName("Testing case insensitive APIs for the cname.")
  void caseInsensitiveTest() throws IOException {

    final String alias = "oneops-test-cs-cname1." + domain();
    final String canonicalName = "oneops-test-cs." + domain();

    // Clean it.
    client.deleteCNameRec(alias);

    List<CNAME> rec = client.getCNameRec(alias);
    assertTrue(rec.isEmpty());

    // Creates CNAME Record
    CNAME cname = client.createCNameRec(alias, canonicalName);
    assertEquals(canonicalName, cname.canonical());

    // Now search with uppercase
    String upperCaseAlias = alias.toUpperCase();

    List<CNAME> cNameRec = client.getCNameRec(upperCaseAlias);
    assertEquals(1, cNameRec.size());
    assertEquals(canonicalName, cNameRec.get(0).canonical());

    // Delete CNAME Record with uppercase.
    List<String> delCName = client.deleteCNameRec(upperCaseAlias);
    assertEquals(1, delCName.size());

    // Make sure the lower case is also deleted.
    List<CNAME> cNameRec1 = client.getCNameRec(alias);
    assertEquals(0, cNameRec1.size());
  }

  @Test
  @DisplayName("Canonical name query tests.")
  void canonicalNameTest() throws IOException {

    final String alias1 = "oneops-test-cn-cname1." + domain();
    final String alias2 = "oneops-test-cn-cname2." + domain();
    final String canonicalName = "oneops-test-cn." + domain();

    // Clean it.
    client.deleteCNameRec(alias1);
    client.deleteCNameRec(alias2);

    List<CNAME> rec = client.getCNameCanonicalRec(canonicalName);
    assertTrue(rec.isEmpty());

    // Creates CNAME Records
    CNAME cname1 = client.createCNameRec(alias1, canonicalName);
    assertEquals(canonicalName, cname1.canonical());

    CNAME cname2 = client.createCNameRec(alias2, canonicalName);
    assertEquals(canonicalName, cname2.canonical());

    // Do a reverse lookup again using canonical name.
    List<CNAME> cRecs1 = client.getCNameCanonicalRec(canonicalName);
    assertEquals(2, cRecs1.size());
    assertTrue(cRecs1.contains(cname1));
    assertTrue(cRecs1.contains(cname2));

    String recRef1 = client.deleteRecord(cname1);
    assertNotNull(recRef1);
    String recRef2 = client.deleteRecord(cname2);
    assertNotNull(recRef2);

    // Do a reverse lookup again.
    List<CNAME> cRecs2 = client.getCNameCanonicalRec(canonicalName);
    assertEquals(0, cRecs2.size());
  }
}
