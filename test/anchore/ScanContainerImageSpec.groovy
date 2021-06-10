/*
  Copyright © 2018 Booz Allen Hamilton. All Rights Reserved.
  This software package is licensed under the Booz Allen Public License. The license can be found in the License file or at http://boozallen.github.io/licenses/bapl
*/

package libraries.anchore

public class ScanContainerImageSpec extends JTEPipelineSpecification {

  def ScanContainerImage = null

  def app_env = [:]
	def newAnchoreImageMock = [
		[
			imageDigest: "sha256:b9ab08c878c9e384835c193fd08c8e25cc7780b3fdb2ad26b144b502ec978eca",
			analysis_status: "analyzed",
			image_detail: [
				imageId: "04fdc20ced95dcafb9120053ee44e936b1d897f177e06d90c3f5e50bf7d56fa2"
			]
		]
	]

  def setup() {
    app_env = [anchore: [docker_registry_credential_id: "docker-registry-appenv"]]
    ScanContainerImage = loadPipelineScriptForStep("anchore","scan_container_image")
    ScanContainerImage.getBinding().setVariable("config", [docker_registry_credential_id: "docker-registry-appenv", usePlugin: true])

    explicitlyMockPipelineVariable("out")
    explicitlyMockPipelineVariable("user")
    explicitlyMockPipelineVariable("pass")
    explicitlyMockPipelineStep("add_registry_creds")
    explicitlyMockPipelineStep("anchore")
    explicitlyMockPipelineStep("get_images_to_build")

    getPipelineMock("get_images_to_build")() >> {
      def images = []
      images << [registry: "reg1", repo: "repo1", context: "context1", tag: "tag1"]
      images << [registry: "reg2", repo: "repo2", context: "context2", tag: "tag2"]
      return images
    }
  }

  def "Calls add_registry_creds when docker_registry_credential_id is provided in Library config" () {
    when:
      ScanContainerImage()
    then:
      1 * getPipelineMock("add_registry_creds")([])
  }

  def "Calls add_registry_creds when docker_registry_credential_id is provided in Application Environment" () {
    setup:
      ScanContainerImage.getBinding().setVariable("config", [usePlugin: true])
    when:
      ScanContainerImage(app_env)
    then:
      1 * getPipelineMock("add_registry_creds")(app_env)
  }

  def "Calls Anchore plugin when usePlugin is true in Library config" () {
    setup:
      ScanContainerImage.getBinding().setVariable("config", [usePlugin: true])
    when:
      ScanContainerImage(app_env)
    then:
      1 * getPipelineMock("anchore").toString()
  }

  def "Calls Anchore plugin with with intended default args" () {
    setup:
      ScanContainerImage.getBinding().setVariable("config", [usePlugin: true])
    when:
      ScanContainerImage(app_env)
    then:
			1 * getPipelineMock("anchore")(_) >> {_arguments -> 
            assert _arguments[0].name == 'anchore_images'
            assert _arguments[0].engineCredentialsId == null
            assert _arguments[0].annotations == []
            assert _arguments[0].policyBundleId == ""
            assert _arguments[0].bailOnFail == true
      }
  }

  def "Calls Anchore plugin with override args when they are present in Library config" () {
    setup:
			def configArgs = [
				usePlugin: true,
				cred: "test-cred",
				annotations: [[key: 'image_owner', value: 'my_team']],
				policyBundleId: "myUUID",
				bailOnFail: false
			]
      ScanContainerImage.getBinding().setVariable("config", configArgs)
    when:
      ScanContainerImage(app_env)
    then:
			1 * getPipelineMock("anchore")(_) >> {_arguments -> 
            assert _arguments[0].name == 'anchore_images'
            assert _arguments[0].engineCredentialsId == configArgs.cred
            assert _arguments[0].annotations == configArgs.annotations
            assert _arguments[0].policyBundleId == configArgs.policyBundleId
            assert _arguments[0].bailOnFail == configArgs.bailOnFail
      }
  }

	def "Skips Anchore plugin when usePlugin is undefined in Library config" () {
    setup:
      ScanContainerImage.getBinding().setVariable("config", [
				perform_vulnerability_scan: false,
				perform_policy_evaluation: false
			])
			explicitlyMockPipelineStep("readJSON")
			getPipelineMock("readJSON")([file: "new_anchore_image.json"]) >> {
				return newAnchoreImageMock
			}
			getPipelineMock("readJSON")([file: "new_anchore_image_check.json"]) >> {
				return newAnchoreImageMock
			}
    when:
      ScanContainerImage(app_env)
    then:
      0 * getPipelineMock("anchore").toString()
  }

}