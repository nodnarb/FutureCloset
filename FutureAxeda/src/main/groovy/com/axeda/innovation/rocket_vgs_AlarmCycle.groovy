import com.axeda.drm.sdk.Context
import groovy.xml.MarkupBuilder
import com.axeda.drm.sdk.data.*
import com.axeda.drm.sdk.device.*
import groovy.json.JsonSlurper
import net.sf.json.JSONObject
import groovy.json.JsonBuilder
import net.sf.json.JSONArray
import com.axeda.common.sdk.id.Identifier
import com.axeda.drm.sdk.scripto.Request
 
/**
* vgs_AlarmCycle.groovy
* -----------------------
*
* Changes the state of an Alarm Entry
*
* @params
*  alarmId (REQUIRED) Str - the id of the alarm
*  state (REQUIRED) Str - new alarm state
*  description (OPTIONAL) alarm change message
*
*
* @author sara streeter <sstreeter@axeda.com>
 *
*/
 
def contentType = "application/json"
 
 
/*
POST
{
  "alarmId": "3",
  "state":"CLOSED"
  }
  
  Which returns me
{
  "modelNumber": "KONTRON_M2MDev",
  "serialNumber": "00B3386EDAC3"
   "alarmId": "3",
  "state":"CLOSED"
}
 */
 
try {
    Context CONTEXT = Context.getSDKContext()
    
     def body = Request.body
    
    def jsonIn = new JsonSlurper().parseText(body)
	
	def alarmId = jsonIn.alarmId
    def state = jsonIn.state
 
    AlarmFinder alarmFinder = new AlarmFinder(CONTEXT);
    HistoricalAlarmFinder halarmFinder = new HistoricalAlarmFinder(CONTEXT);
 
    alarmFinder.setId(new Identifier(alarmId.toLong()));
    halarmFinder.setId(new Identifier(alarmId.toLong()))
 
    def alarm = alarmFinder.find() ? alarmFinder.find() : halarmFinder.find()
    
    StringBuilder outputString = new StringBuilder()
    
    if (alarm == null){
        outputString.append("{\n  \"error\": \"No alarm found for id ${alarmId}\"}\n")
        
    }
    else {
        def alarmstate = state.toUpperCase() as AlarmState
     
        if (alarm && alarmstate) { 
            alarm.setState(alarmstate, ""); 
        }
        
        outputString.append("{\n")
        outputString.append("  \"modelNumber\": \"${alarm.device?.model?.name}\",\n")
        outputString.append("  \"serialNumber\": \"${alarm.device?.serialNumber}\",\n")
        outputString.append("  \"alarmId\": \"${alarm.id}\",\n")
        outputString.append("  \"state\": \"${alarm.state}\"\n")
        outputString.append("\n}")
    }
	
	return ["Content-Type": contentType,"Content":outputString]
    
} catch (Exception ex)
{
    logger.info ex.getMessage();
 }
