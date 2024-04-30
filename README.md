# AppDynamics CSA License

[![published](https://static.production.devnetcloud.com/codeexchange/assets/images/devnet-published.svg)](https://developer.cisco.com/codeexchange/github/repo/FHDumont/appdynamics-csa-license-extension)

This extension works only with the standalone machine agent.

The purpose of this extension is to analyze applications enabled with CSA and to count the usage of licenses. Additionally, metrics of consumption are created and a dashboard for visualization is set up.

## Requirements

1. Before the extension is installed, the prerequisites mentioned [here](https://community.appdynamics.com/t5/Knowledge-Base/Extensions-Prerequisites-Guide/ta-p/35213) need to be met. Please do not proceed with the extension installation if the specified prerequisites are not met.
2. Username and password capable of connecting to the Controller and the CSA feature.
3. The server running the machine agent must have at least 4 vCPUs and 8 GB of RAM.

## Installation

1. Run 'mvn clean install' from the appdynamics-csa-license-extension directory or copy the already compiled extension by [clicking here](https://github.com/FHDumont/appdynamics-csa-license-extension/releases/download/v1.0/CSALicenseExtension-1.0.zip)
2. Deploy the file CSALicenseExtension.zip found in the 'target' directory into \<machineagent install dir\>/monitors/
3. Unzip the deployed file
4. Open the \<machineagent install dir\>/monitors/CSALicenseExtension/config.yml file and update the host, username, and password for each existing controller.
5. You can configure the format of the date and time in the tags corresponding to dates by simply changing the formatDate parameter; the default value is dd/MM/yyyy HH:mm:ss.
6. After the correlation process finishes, it will wait for the time configured in the \<machineagent install dir\>/monitors/CSALicenseExtension/monitor.xml file and the execution-frequency-in-seconds property. Feel free to make any changes as needed.
7. Restart the machineagent

Please place the extension in the "monitors" directory of your Machine Agent installation directory. Do not place the extension in the "extensions" directory of your Machine Agent installation directory.

## How to use

The time required to perform the CSA License will depend on the total number of controllers, appliations, tiers and nodes. This time can range from a few seconds to several minutes.

No further action is required to obtain results; simply wait for the process to finish. After the process is completed, the information can be verified as shown in the images below.

![01](https://github.com/FHDumont/appdynamics-csa-license-extension/blob/main/doc-images/CSALienseDashboard.png?raw=true)

![02](https://github.com/FHDumont/appdynamics-csa-license-extension/blob/main/doc-images/MetricBrowser.png?raw=true)
