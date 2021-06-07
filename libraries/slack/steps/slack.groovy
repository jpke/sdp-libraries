/*
  Copyright © 2018 Booz Allen Hamilton. All Rights Reserved.
  This software package is licensed under the Booz Allen Public License. The license can be found in the License file or at http://boozallen.github.io/licenses/bapl
*/

package libraries.slack.steps

def call(customMessage = ""){
    switch(currentBuild.result){
        case null: // no result set yet means success
        case "SUCCESS":
          message = customMessage ?: "Build Successful: ${env.JOB_URL}"
          slackSend color: "good", message: message
          break;
        case "FAILURE":
          message = customMessage ?: "Build Failure: ${env.JOB_URL}"
          slackSend color: '#ff0000', message: message
          break;
        case "UNSTABLE":
          message = customMessage ?: "Build Unstable: ${env.JOB_URL}"
          slackSend color: '#FFFF00', message: message
          break;
        default:
          echo "Slack Notifier doing nothing: ${currentBuild.result}"
    }
}
