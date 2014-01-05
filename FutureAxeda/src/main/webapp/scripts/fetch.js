/**
 * App.Fetch
 *
 * Module for fetching pours.
 */
var App = App || {};
App.Fetch = (function (Fetch, $) {

    /**************************************************
     *  Make scripto call to get json response
     *************************************************/
    Fetch.scriptoFetchModels = function () {
        axeda.callScripto("GET", "scriptname", {/* parameters */}, 2, function (json) {

        }, {
            localstoreoff: "yes",
            contentType: "application/json; charset=utf-8"
        })
    }

    return Fetch;
}(App.Fetch || {}, jQuery));