/**************************************
 * vgsAlarms.js
 * All functionality for  alarms page
 *************************************/

var vgsAlarms = (function (vgsAlarms, $, undefined) {
vgsAlarms.debug = null
    /**************************************************
     *  first point of entry to start building page
     *************************************************/
    vgsAlarms.processJson = function(json, div) {
        fleetAlarmBuilder(json, div)
        clickHandlers(div)
    }

    /**************************************************
     *  Builds array of alarms  information
     *************************************************/
    function fleetAlarmBuilder(json, div) {
        var alarmArr = []
        if (json.alarms && json.alarms.length > 0) {
            for (var j = 0; j < json.alarms.length; j++) {
                alarmArr.push({
                    "name": json.alarms[j].name,
                    "time": json.alarms[j].timestamp,
                    "state": json.alarms[j].state,
                    "id": json.alarms[j].id
                })
            }
        }
        alarmArr.sort(function (a, b) {
            return (a.time < b.time) ? 1 : ((b.time < a.time) ? -1 : 0);
        });
        alarmTableBuilder(alarmArr, div)
    }

    /**************************************************
     *  Builds table of alarms
     *************************************************/
    function alarmTableBuilder(alarmArr, alarmdiv) {
        $(alarmdiv).html('')
        for (var i = 0; i < alarmArr.length; i++) {
            $(alarmdiv).append(
                '<div class="alertbox clearfix alarmstate' + alarmArr[i].state + '" data-id="' + alarmArr[i].id  + '">' +
                    '<img class="alarmicon" src="images/alarmssolid.png"/>' +
                    '<div class="alarmtext"><b>' + alarmArr[i].name +
                    '<div class="alarmtime">' + formatFancyDate(alarmArr[i].time) + '</div>' +
                    '</div><div class="alarmControl">' +
                    '<img name="accept" src="images/accept.png" title="Acknowledge"/>' +
                    '<img name="close"src="images/close.png" title="Close"/>' +
                    '<div class="alarmWrapper clearfix">' +
					'</div></div>' 
            )
        }
        if (alarmArr.length == 0 || $(alarmdiv).find(' .alertbox:visible').length == 0)
            $(alarmdiv).prepend('<div class="noalarms"><span>No current alarms available</span><img src="images/alarmslarge.png"></div>')

    }

    /************************************
     *   if description return text
     ***********************************/
    function alarmDescription(desc) {
        if (desc != null)return '<br/><br/>Description:<br>"' + desc + '"'
        return ""
    }

    /************************************
     *   formats timestamp as fancy date
     ***********************************/
    function formatFancyDate(date) {
        if (isNaN(date))
            return ""
        date = parseInt(date)
        var monthNames = [ "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" ]
        var formatDate = new Date(date)
        var curr_date = formatDate.getDate();
        var curr_month = monthNames[formatDate.getMonth()];
        var curr_year = formatDate.getFullYear();
        var hour = formatDate.getHours();
        var mins = formatDate.getMinutes();
        var ampm = "am"
        if (hour > 12) {
            hour -= 12
            ampm = "pm"
        }
        if (mins < 10)
            mins = "0" + mins
        var newString = '<span class="time">' + hour + ':' + mins + ampm + '</span><br><span class="date">' + get_nth_suffix(curr_date) + " " + curr_month + ' ' + curr_year + '</span>'
        return newString

        function get_nth_suffix(date) {
            switch (date) {
                case 1:
                case 21:
                case 31:
                    return date + '<sup>st</sup>';
                case 2:
                case 22:
                    return date + '<sup>nd</sup>';
                case 3:
                case 23:
                    return date + '<sup>rd</sup>';
                default:
                    return date + '<sup>th</sup>';
            }
        }
    }

    /**************************************************
     *  set border color based on state
     *************************************************/
    function stateColor(state) {
        if (state == "acknowledged")return '#2D8926'
        if (state == "closed")return '#a60000'
        var col = $('.ax_section').css('color')
        return col
    }

    /************************************
     *   change alarm state call
     ***********************************/
    function changeAlarmState(id, state, div, widgetdiv) {
	service.call('vgs_AlarmCycle', {
            "alarmId": id,
            "state": state
        }, function (e) {
            div.fadeOut(function () {
                if ($(widgetdiv).find('.alertbox:visible').length == 0)
                    $(widgetdiv).append('<div class="noalarms"><span>No current alarms available</span><img src="images/alarmslarge.png"></div>')
                else
                    $(widgetdiv).find('.noalarms').remove()
            })
            $(widgetdiv).prepend('<div class="message" style="display:none"><span>ALARM ' + state + '</span></div>')
            $(widgetdiv).find('.message').fadeIn('slow').delay(1000).fadeOut('slow', function () {
                this.remove()
            })
        })
    }

    /************************************
     *   all button control for page
     ***********************************/
    function clickHandlers(div) {
        $(div).find('.alarmControl img').click(function (e) {
            e.preventDefault()
            var alarmid = $($(this).parent().closest('.alertbox')).attr("data-id")
            var name = $(this).attr('name')
            if (name == 'accept')
                changeAlarmState(alarmid, 'ACKNOWLEDGED', $(this).parent().closest('.alertbox'), div)
            if (name == 'close')
                changeAlarmState(alarmid, 'CLOSED', $(this).parent().closest('.alertbox'), div)
        })
    }


    return vgsAlarms;
}(window.vgsAlarms || {}, jQuery));