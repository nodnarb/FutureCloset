import com.axeda.drm.sdk.Context
import groovy.xml.MarkupBuilder
import com.axeda.drm.sdk.data.*
import com.axeda.drm.sdk.device.*
import groovy.json.JsonSlurper
import net.sf.json.JSONObject
import groovy.json.JsonBuilder
import net.sf.json.JSONArray
import com.axeda.drm.sdk.scripto.Request

/**
* vgs_GetAlarms.groovy
* -----------------------
*
* Gets all started alarms
*
* @params via POST
*  modelNumber (REQUIRED) Str - the model of the asset
*  serialNumber (REQUIRED) Str - new alarm state
*  maxrows (OPTIONAL) Int - max num of alarms to fetch
*  excludealarm (OPTIONAL) List - list of alarm names to exclude alarms
*
*
* @author sara streeter <sstreeter@axeda.com>
 *
*/
 
def contentType = "application/json"


/*
POST
{
  "modelNumber": "KONTRON_M2MDev",
  "serialNumber": "00B3386EDAC3"
  "maxrows": "3",
  "excludealarm":["nametoexclude"]
  }
  
  Which returns me
{
  "modelNumber": "KONTRON_M2MDev",
  "serialNumber": "00B3386EDAC3"
  "maxrows": "3",
"alarms":[{"name":"Alarm1","timestamp":"123456"}, {"name":"Alarm2","timestamp":"123455"}]
}
 */
 
try
{
 
    final def CONTEXT = Context.getSDKContext()
    def body = Request.body
    
    def jsonIn = new JsonSlurper().parseText(body)
    
	def modelNumber = jsonIn.modelNumber
    def serialNumber = jsonIn.serialNumber
    modelNumber = "KONTRON_M2MDev"

    def maxrows = jsonIn.maxrows ? jsonIn.maxrows : 3
	def excludealarm = jsonIn.excludealarm ? jsonIn.excludealarm : []
    
    ModelFinder modelFinder = new ModelFinder(CONTEXT)
    modelFinder.setName(modelNumber)
    Model model = modelFinder.find()

    // THe Finder acts as the Criteria for finding an object. You can use code completion to see the available criteria
    DeviceFinder deviceFinder = new DeviceFinder(CONTEXT)
    deviceFinder.setModel(model)
    deviceFinder.setSerialNumber(serialNumber)
    Device device = deviceFinder.find()
    
    List alarms
    
  if (device != null){

      AlarmFinder aFinder = new AlarmFinder(CONTEXT)
//      HistoricalAlarmFinder halarmFinder = new HistoricalAlarmFinder(CONTEXT);
      aFinder.setDevice(device)
      aFinder.setState(AlarmState.STARTED)
      
      alarms = aFinder.findAll()
      
      def length = maxrows < alarms?.size() ? maxrows-1 : alarms?.size() - 1
      
      alarms = alarms.sort{ a,b ->  b.date <=> a.date }.findAll{ !excludealarm.contains(it.name) }
      
      if (length > 0){
          alarms = alarms[0..length]
      }
  }
  
    StringBuilder outputString = new StringBuilder()
    outputString.append("{\n")
    outputString.append("  \"modelNumber\": \"${device?.model?.name}\",\n")
    outputString.append("  \"serialNumber\": \"${device?.serialNumber}\",\n")
    outputString.append("  \"maxrows\": \"${maxrows}\",\n")
    outputString.append("  \"alarms\": [")
    def first = true
    alarms?.each() { alarm ->
        if (first) {
            first = false
        } else {
            outputString.append("\t,")
        }
        def alarmText = ""
        if (alarm.name?.length()>50) {
            alarmText = alarm.name.substring(0,50)+ "..."
        } else {
            alarmText = alarm.name
        }

		outputString.append("{\"id\":\"${alarm.id}\",\"name\":\"${alarmText}\",\"timestamp\":\"${alarm.date.time}\"}\n")

    }
    outputString.append("]\n}")
	
	return ["Content-Type": contentType,"Content":outputString]
 
}catch (Exception ex)
{
    logger.info ex.getMessage();
 }
return true
