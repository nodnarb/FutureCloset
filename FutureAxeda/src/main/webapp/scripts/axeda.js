/**************************************
 * axeda.js
 * web services plugin
 * features:
 *  -login
 *  -logout
 *  -callScripto
 *************************************/
// 
// functions and variables pertaining to axeda services
//
// author: philip lombardi
// created on: august 29, 2011

// using the enterprise jquery namespace methodology see below:
// http://enterprisejquery.com/2010/10/how-good-c-habits-can-encourage-bad-javascript-habits-part-1/

var axeda = (function (axeda, $, undefined) {
    axeda.username = ""
    PASSWORD = ""

    var PLATFORM_HOST = document.URL.split('/apps/')[0];
    var SERVICES_PATH = '/services/v1/rest/';
    var APP_NAME = window.location.protocol !== 'file:'?document.URL.split('/apps/')[1].split('/')[0]:""

    var SESSION_ID = null;
    var SESSION_EXPIRATION = 60 * 60 * 1000;

    var STOREINTERVAL = 5

    function setHost() {
        if (window.location.protocol === 'file:') {
            return  '.';
        }//'.'; }
        if (window.location.hostname === '127.0.0.1') {
            return PLATFORM_HOST;
        }
        return PLATFORM_HOST;
    };

    axeda.host = setHost();

    axeda.doLogin = function (username, password, success, failure) {
        return login(username, password, success, failure);
    };

    function login(username, password, success, failure) {
        var reqUrl = axeda.host + SERVICES_PATH + 'Auth/login';
        localStorage.clear()
        return $.get(reqUrl, {
            'principal.username': username,
            'password': password
        },function (xml) {
            var sessionId = $(xml).find("ns1\\:sessionId, sessionId").text()
            // var sessionId = $(xml).find("[nodeName='ns1:sessionId']").text(); - no longer works past jquery 1.7
            if (sessionId) {
                // set the username and password vars for future logins.
                axeda.username = username
                PASSWORD = password

                storeSession(sessionId);
                success(SESSION_ID); // return the freshly stored contents of SESSION_ID
            } else {
                failure($(xml).find("faultstring").text());
            }
        }).error(function () {
                $('#loginerror').html('Login Failed, please try again')
            });
    };

    axeda.loginWithCookie = function () {
        var cookiePiece;
        var cookieArray = document.cookie.split(';');
        var i;
        var pos;
        var len = APP_NAME.length + 11  //11 is  _sessionId= string length

        for (i = 0; i < cookieArray.length; i++) {
            cookiePiece = cookieArray[i];
            if ((pos = cookiePiece.indexOf(APP_NAME+'_sessionId=')) >= 0) {
                SESSION_ID = cookiePiece.substring(pos + len, cookiePiece.length);
                return true;
            }
        }
        return false;
    };


    function expiredSessionLogin() {
        login(axeda.username, PASSWORD, function () {
        }, function () {
        })
    };

    function storeSession(sessionId) {
        var date = new Date();
        date.setTime(date.getTime() + SESSION_EXPIRATION);
        SESSION_ID = sessionId
        document.cookie = APP_NAME+'_sessionId=' + SESSION_ID + '; expires=' + date.toGMTString() + '; path=/';
        return true;
    };

    axeda.doLogout = function () {
        logout();
    };

    function logout() {
        document.cookie = APP_NAME+'_sessionId=; expires=' + new Date().toGMTString() + '; path=/';
        SESSION_ID = null;
        SESSION_EXPIRATION = null;
        localStorage.clear();
        axeda.username = ""
        PASSWORD = ""
    };


    /**
     * makes a call to the enterprise platform services with the name of a script and passes
     * the script any parameters provided.
     *
     * default is GET if the method is unknown
     *
     * Notes: Added POST semantics - plombardi @ 2011-09-07
     *
     * original author: Zack Klink & Philip Lombardi
     * added on: 2011/7/23
     */
        // options - localstoreoff: "yes" for no local storage, contentType: "application/json; charset=utf-8",
    axeda.callScripto = function (method, scriptName, scriptParams, attempts, callback, options) {
        var reqtime = new Date().getTime()
        axeda.loading(true, reqtime)
        var reqUrl = axeda.host + SERVICES_PATH + 'Scripto/execute/' + scriptName + '?sessionid=' + SESSION_ID
        var contentType = options.contentType ? options.contentType : "application/json; charset=utf-8"
        var local
        var daystring = keygen()
        if (options.localstoreoff == null) {
            if (localStorage) {
                local = localStorage.getItem(scriptName + JSON.stringify(scriptParams))
            }
            if (local != null && local == daystring) {
                return dfdgen(reqUrl + JSON.stringify(scriptParams))
            }
            else {
                localStorage.setItem(scriptName + JSON.stringify(scriptParams), daystring)
            }
        }

        return $.ajax({
            type: method,
            url: reqUrl,
            data: scriptParams,
            contentType: contentType,
            dataType: "text",
            cache: false,
            error: function () {
                if (attempts) {
                    expiredSessionLogin();
                    setTimeout(function () {
                        axeda.callScripto('POST', scriptName, scriptParams, attempts - 1, callback, options)
                    }, 1500);
                }
                else {
                    logout()
                    location.reload(true)
                }
            },
            success: function (data) {
                axeda.loading(false, reqtime)
                if (options.localstoreoff == null) {
                    localStorage.setItem(reqUrl + JSON.stringify(scriptParams), JSON.stringify([data]))
                }
                if (contentType.match("json")) {
                    callback(unwrapResponse(data))
                }
                else {
                    callback(data)
                }

            }

        })

    };

    /**
     * App.loading(loading?: boolean)
     *
     * Toggle the application's "loading screen" on or off. Pass true to
     * fade the loading screen in, false to immediately hide the loading
     * screen. The default is false.
     */
    axeda.loading = function(loading, reqtime) {
        if ($('#loader' + reqtime).length == 0)
            $('body').append('<div class="ax_loader" id="loader' + reqtime + '" ' +
                'style="top:0;left:0;right:0;bottom:0;width:100%;height:100%;height: auto;position: fixed;' +
                'background: rgba(99,99,99,0.2);z-index:999999">' +
                '<img src="images/loader.gif" style=" position: absolute;top: 50%;left: 50%;' +
                'margin-left: -64px;margin-top: -64px;"></div>')
        if (loading) {
            $('#loader' + reqtime).css('opacity', 0).show().animate({
                opacity: 1
            }, 500)
        } else {
            $('#loader' + reqtime).stop(true, false).remove()
        }
    };

    //create a deferred object to work with .then
    function dfdgen(reqUrlparam) {
        var dfd = $.Deferred
        dfd.resolve
        return localStorage.getItem(reqUrlparam)
    }

    // generate a key from the timestamp for local storage of json
    function keygen() {
        var d = new Date();
        var minute = d.getMinutes()
        var remainder = minute % STOREINTERVAL
        minute = minute - remainder
        if (minute.length < 2) minute = '0' + minute
        var hour = d.getHours()
        if (hour.length < 2) hour = '0' + hour
        var monthn = d.getMonth() + 1
        var month = monthn.toString()
        if (month.length < 2) month = '0' + month
        var day = d.getDate().toString()
        if (day.length < 2) day = '0' + day
        var year = d.getFullYear().toString()

        var dayString = year + month + day + hour + minute
        return dayString
    }

    // utility method to unwrap a response which might have been returned as a string rather
    // than JSON.
    function unwrapResponse(json) {
        if (typeof json === 'string') {
            json = $.parseJSON(json);
        }
        if (json.wsScriptoExecuteResponse) {
            json = $.parseJSON(json.wsScriptoExecuteResponse.content)
        }

        return json;
    };


    return axeda;
}(window.axeda || {}, jQuery));