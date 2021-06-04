/*
  Copyright © 2018 Booz Allen Hamilton. All Rights Reserved.
  This software package is licensed under the Booz Allen Public License. The license can be found in the License file or at http://boozallen.github.io/licenses/bapl
*/

package libraries.google_lighthouse

public class AccessibilityComplianceScanSpec extends JTEPipelineSpecification {

  def AccessibilityComplianceScan = null

  def setup() {
    AccessibilityComplianceScan = loadPipelineScriptForStep("google_lighthouse","accessibility_compliance_scan")
    explicitlyMockPipelineStep("inside_sdp_image")
  }

  def "Error is thrown if url is not provided" () {
    setup:
      AccessibilityComplianceScan.getBinding().setVariable("config", [url: null])
    when:
      AccessibilityComplianceScan()
    then:
      1 * getPipelineMock("error")(_)
  }

  def "Application Environment url is used if present" () {
    setup:
        def app_env = [google_lighthouse: [url: "app_env url"]]
      AccessibilityComplianceScan.getBinding().setVariable("config", [url: "config url"])
    when:
      AccessibilityComplianceScan(app_env)
    then:
      1 * getPipelineMock("sh")({it =~ / lighthouse app_env url/})
  }


  def "Library config url is used if Application Environment is not present" () {
    setup:
      AccessibilityComplianceScan.getBinding().setVariable("config", [url: "config url"])
    when:
      AccessibilityComplianceScan()
    then:
      1 * getPipelineMock("sh")({it =~ / lighthouse config url/})
  }

}