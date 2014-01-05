import com.axeda.drm.sdk.Context
import groovy.xml.MarkupBuilder
import com.axeda.drm.sdk.data.*
import com.axeda.drm.sdk.device.*
 
/**
* vgs_CloseAlarms.groovy
* -----------------------
*
* Closes out all started alarms with the alarm name provided
*
* @params
*  alarmname (REQUIRED) Str - the name of the alarm
*
*
* @author sara streeter <sstreeter@axeda.com>
 *
*/
 
try
{
 
    final def CONTEXT = Context.getSDKContext()
    Device device = context.device;
	AlarmFinder aFinder = new AlarmFinder(CONTEXT)
      aFinder.setDevice(device)
      aFinder.setAlarmName(parameters.alarmname)
      aFinder.setState(AlarmState.STARTED)
      
      def alarms = aFinder.findAll()
      
  HistoricalAlarmFinder halarmFinder = new HistoricalAlarmFinder(CONTEXT);  
  halarmFinder.setDevice(device)
  halarmFinder.setAlarmName(parameters.alarmname)
  halarmFinder.setState(AlarmState.STARTED)
  
  def alarms = halarmFinder.findAll()
  
  for (Alarm alarm: alarms) {
      alarm.setState(AlarmState.CLOSED, "")
    
  }
 
}catch (Exception ex)
{
    logger.info ex.getMessage();
 }
return true
