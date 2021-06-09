/*
  Copyright © 2018 Booz Allen Hamilton. All Rights Reserved.
  This software package is licensed under the Booz Allen Public License. The license can be found in the License file or at http://boozallen.github.io/licenses/bapl
*/

package libraries.anchore

public class ScanContainerImageSpec extends JTEPipelineSpecification {

  def ScanContainerImage = null

  def app_env = [:]

  def setup() {
    app_env = [anchore: [docker_registry_credential_id: "docker-registry-appenv"]]
    ScanContainerImage = loadPipelineScriptForStep("anchore","scan_container_image")
    ScanContainerImage.getBinding().setVariable("config", [docker_registry_credential_id: "docker-registry-appenv", usePlugin: true])

    // ScanContainerImage.getBinding().setVariable("env", env)
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
      1 * getPipelineMock("anchore")(*)
      1 * getPipelineMock("anchore")([name:'anchore_images', engineCredentialsId:null, annotations:[], policyBundleId:'', bailOnFail:true])
  }

  // def "Calls Anchore plugin with bailOnFail = true by default" () {
  //   setup:
  //     ScanContainerImage.getBinding().setVariable("config", [usePlugin: true])
  //   when:
  //     ScanContainerImage(app_env)
  //   then:
	// 		1 * getPipelineMock("anchore")(_) >> {_arguments -> 
  //           assert _arguments[0][5] == [bailOnFail:true]
  //     }
  // }

//   def "Fails if npm method is not listed in package.json scripts" () {
//     setup:
//       ScanContainerImage.getBinding().setVariable("config", [unit_test: [script: "not_found"]])
//     when:
//       ScanContainerImage("unit_test")
//     then:
//       1 * getPipelineMock("error")("stepName 'not_found' not found in package.json scripts")
//   }
}