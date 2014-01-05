var ax_app = {
	PLATFORM_HOST : document.URL.split('/apps/')[0],
    defaultModelName: 'KONTRON_M2MDev',
    defaultSerialNumber: '16820870',
    appjson: null,
    editing: false,
	debug:null,
	files:[],
	fileinput:null,
	logo:[],
	logoUrl: "",
    refreshrate:1000,
    initilize: function () {
        service.call('rocket_vgs_GetSettings', {
            "modelNumber": ax_app.defaultModelName,
            "serialNumber": ax_app.defaultSerialNumber
        }, ax_app.buildapp)
        this.nav()
    },
    buildapp: function (json) {
        if (json)ax_app.appjson = json
        $('.ax_header .title').html(decodeMe(ax_app.appjson.application.appName) || 'Axeda Hackathon')
        $('.ax_header .logo').attr('src', ax_app.appjson.application.logoUrl || "")
		
		checkConnectedStatus()

        if (document.location.hash == '#settings' || ax_app.appjson.layout.widgets.length == 0) {
            $('.ax_settingsbody').fadeIn()
            $('.ax_mainbody').hide()
            $('.ax_header .navbtn span').html('Application')
            ax_app.settings()
        }
        else {
            $('.ax_settingsbody').hide()
            $('.ax_mainbody').html('').fadeIn()
			
            $(ax_app.appjson.layout.widgets).each(function (pos, type) {
                $('.ax_mainbody').append(eval('ax_widget_' + type.type).holder(pos, type) || null)
                eval('ax_widget_' + type.type).build(pos, type)
                eval('ax_widget_' + type.type).run(pos, type)
            })
        }
    },
    nav: function () {
        $('.ax_header .navbtn').click(function (e) {
            e.preventDefault()
            if ($('.ax_mainbody').is(':visible')) {
                document.location.hash = 'settings'
                $('.ax_settingsbody').fadeIn()
                $('.ax_mainbody').hide()
                $(this).find('span').html('Application')
                ax_app.settings()
            }
            else if ($('.ax_settingsbody').is(':visible')) {
                document.location.hash = ''
                $('.ax_settingsbody').hide()
                $('.ax_mainbody').fadeIn()
                $(this).find('span').html('Settings')
                location.reload()
            }
        })

        $('#ax_addwidget').unbind('click').click(function (e) {
            e.preventDefault()
            if (!ax_app.validate())return false
            var widget = {
                dataitemName: $('#ax_widgetdataitem').val(),
                title: encodeMe($('#ax_widgettitle').val()),
                type: $('#ax_widgettype').val(),
                width: $('input[name=ax_widgetsize]:checked').val()
            }
			if ($('#ax_widgetcontent').val().trim()){
				widget.content = encodeMe($('#ax_widgetcontent').val())
			}

			if (ax_app.files.length > 0){
				var tag = $(ax_app.fileinput).attr("data-type")
				handleFile(ax_app.files, "MODEL:" + ax_app.appjson.device.modelNumber + ",SERIAL:" + ax_app.appjson.device.serialNumber + ",TYPE:" + tag)
			}
            if (ax_app.editing || ax_app.editing===0) ax_app.appjson.layout.widgets[ax_app.editing] = widget
            else ax_app.appjson.layout.widgets.push(widget)
			
            ax_app.buildsettingstable()
            ax_app.reset()
        })

        $('#ax_savesettings').unbind('click').click(function (e) {
            e.preventDefault()
			ax_app.settingsCallback()
            
			
        })

        $('#ax_widgettype').change(function () {
            var sel = $(this).find(':selected').val()
			eval('ax_widget_' + sel).settings()
        })

    },
    reset: function () {
        ax_app.editing = false
        $('#ax_widgettitle').val('')
        $('#ax_widgettype').val('')
        $('input[type=radio][name=ax_widgetsize][value="100"]').prop('checked', true)
        $('#ax_widgetdataitem').val('')
		$('#ax_widgetcontent').val('')
    },

    settings: function () {
        $('#ax_appname').val(decodeMe(ax_app.appjson.application.appName))
        this.reset()
        this.buildsettingstable()
        this.fetchdataitems()
    },
    buildsettingstable: function () {
        var widgets = ax_app.appjson.layout.widgets
        var r = new Array(), j = -1;
        r[++j] = '<thead><th>Title</th><th>Type</th><th>Width</th><th>Data Item</th><th style="width:155px"></th></thead>'
        for (var i = 0; i < widgets.length; i++) {
            r[++j] = '<tr><td>';
            r[++j] = widgets[i].title
            r[++j] = '</td><td>';
            r[++j] = widgets[i].type
            r[++j] = '</td><td>';
            r[++j] = widgets[i].width
            r[++j] = '</td><td>';
            r[++j] = widgets[i].dataitemName
            r[++j] = '</td><td style="width:155px">';
            r[++j] = '<div class="ax_tablebtn" name="edit"></div>'
            r[++j] = '<div class="ax_tablebtn" name="delete"></div>'
            r[++j] = '<div class="ax_tablebtn" name="up"></div>'
            r[++j] = '<div class="ax_tablebtn" name="down"></div>'
            r[++j] = '</td></tr>';
        }
        $('#ax_currentwidgets').html(r.join(''))
		var sel = $('#ax_widgettype').find(':selected').val() ? $('#ax_widgettype').find(':selected').val() : $('#ax_widgettype').first().val()
		if (sel){
			eval('ax_widget_' + sel).settings()
		}
		
		$("#ax_logoupload").off('change.fileinput').on('change.fileinput', function(event){
			if (this.files && this.files.length > 0){
				ax_app.logo = this.files
				var f = this.files[0]
				// Only process image files.
				  if (!f.type.match('image.*')) {
					alert("Please select an image file");
					return
				  }
				  
				  var reader = new FileReader();

				  // Closure to capture the file information.
				  reader.onload = (function(theFile) {
					return function(e) {
					  var preview = ['<img class="appImage" ', 'src="', e.target.result,
										'" title="', escape(theFile.name), '"/>'].join('');
					  $(".iconholder").html(preview)
					  var img = $(".iconholder > img")[0]
					  img.onload = function(){
							var maxWidth = 280
							var maxHeight = 280
							scaleImg(img, maxWidth, maxHeight)									
						}

					};
				  })(f);
				  // Read in the image file as a data URL.
				  reader.readAsDataURL(f);
			}
		})
				

        $('.ax_tablebtn').unbind('click').click(function (e) {
            e.preventDefault()
            var tar = $(e.target).attr('name')
            var row = $(this).parents("tr:first");
            if (tar == 'edit') {
                var pos = $(row).prevAll().length
                ax_app.editing = pos
                $('#ax_widgettitle').val(decodeMe(ax_app.appjson.layout.widgets[pos].title))
                $('#ax_widgettype').val(ax_app.appjson.layout.widgets[pos].type)
                $('input[type=radio][name=ax_widgetsize][value="' + "" + ax_app.appjson.layout.widgets[pos].width + '"]').prop('checked', true)
                $('#ax_widgetdataitem').val(ax_app.appjson.layout.widgets[pos].dataitemName)
				if (ax_app.appjson.layout.widgets[pos].content){
					$('#ax_widgetcontent').val(decodeMe(ax_app.appjson.layout.widgets[pos].content))
				}
				var sel = $('#ax_widgettype').find(':selected').val() ? $('#ax_widgettype').find(':selected').val() : $('#ax_widgettype').first().val()
				if (sel){
					eval('ax_widget_' + sel).settings()
				}
            }
            else if (tar == 'delete') {
                ax_app.appjson.layout.widgets.splice($(row).prevAll().length, 1)
                row.fadeOut(function () {
                    this.remove()
                })
            }
            else if (tar == 'up') {
                ax_app.moveRowUp(row)
            }
            else if (tar == 'down') {
                ax_app.moveRowDown(row)
            }
        })
    },
    moveRowUp: function (row) {
        ax_app.widgetObjOrderChange($(row).prevAll().length, 'up')
        row.hide()
        row.insertBefore(row.prev());
        row.fadeIn()
    },
    moveRowDown: function (row) {
        ax_app.widgetObjOrderChange($(row).prevAll().length, 'down')
        row.hide()
        row.insertAfter(row.next());
        row.fadeIn()
    },
    widgetObjOrderChange: function (rowpos, direction) {
        if ((rowpos == 0&&direction == 'up') || (rowpos + 1 == ax_app.appjson.layout.widgets.length && direction == 'down') )return
        var newpos
        if (direction == 'up') newpos = rowpos - 1
        else if (direction == 'down') newpos = rowpos + 1
        var temp = ax_app.appjson.layout.widgets[rowpos];
        ax_app.appjson.layout.widgets[rowpos] = ax_app.appjson.layout.widgets[newpos];
        ax_app.appjson.layout.widgets[newpos] = temp;
    },
    fetchdataitems: function () {
        service.call('rocket_vgs_ListDataItems', {
            "modelNumber": ax_app.appjson.device.modelNumber,
            "serialNumber": ax_app.appjson.device.serialNumber
        }, function (data) {
            $('#ax_widgetdataitem').html('')
            $.each(data.dataItemNames, function (k, v) {
                $('#ax_widgetdataitem').append('<option value=' + v + '>' + v + '</option>')
            })

        })
    },
    validate: function () {
        $('.ax_error').removeClass('ax_error')
        if ($('#ax_widgetdataitem').val().trim().length == 0)$('#ax_widgetdataitem').addClass('ax_error')
        if ($('#ax_widgettitle').val().trim().length == 0)$('#ax_widgettitle').addClass('ax_error')
        if ($('#ax_widgettype').val().trim().length == 0)$('#ax_widgettitle').addClass('ax_error')

        if ($('.ax_error').length > 0)return false
        else return true
    },
	sendDownstream : function(command, btnid){
		service.call('rocket_vgs_SendDownstreamCmd', {
            "modelNumber": ax_app.appjson.device.modelNumber,
            "serialNumber": ax_app.appjson.device.serialNumber,
            "message": command
        })
		var value = $("#" + btnid).val()
		$("#" + btnid).val('Sent')
		setTimeout(function(){
			$("#" + btnid).val(value)
		},ax_app.refreshrate)
	},
	settingsCallback: function(){
		if ($("#ax_appname").val().trim()){
			ax_app.appjson.application.appName = encodeMe($("#ax_appname").val())
		}
		if (ax_app.logo.length > 0){
			handleFile(ax_app.logo, "MODEL:" + ax_app.appjson.device.modelNumber + ",SERIAL:" + ax_app.appjson.device.serialNumber + ",TYPE:LOGO", function(json){
				ax_app.appjson.application.logoUrl = json[0].url
				service.call('rocket_vgs_UpdateSettings', ax_app.appjson, ax_app.buildapp)
			})
		}
		else {
			service.call('rocket_vgs_UpdateSettings', ax_app.appjson, ax_app.buildapp)
		}

	}

}

var checkConnectedStatus = function(){
	service.call('rocket_vgs_GetDeviceStatus', {
            "modelNumber": ax_app.defaultModelName,
            "serialNumber": ax_app.defaultSerialNumber
        },function(json){
			if (json){
				ax_app.appjson.device = json.device
			}
			var i = 0
			$.each(ax_app.appjson.device, function(index, el){
				if (index.indexOf("Status") > -1){
					toggleAlert("idex_0" + i, el)
					i++
				}
			})
			$("#idex_lastcontact").html(ax_app.appjson.device.lastContact)
			setTimeout(checkConnectedStatus,ax_app.refreshrate)
	})
}

var toggleAlert = function(div, status){
	if (status == "Missing"){
		$("#" + div).addClass("missing_class").removeClass("connected_class")
	} else if (status == "Connected"){
		$("#" + div).removeClass("missing_class").addClass("connected_class")
	}

}

	/**************************************************
     *  Upload an image
     *************************************************/
    var handleFile = function (file, tag, callback) {
		file = file[0]
		var formData = new FormData();
		var filename = file.name
		formData.append(filename, file)
		 var url = '/services/v1/rest/Scripto/execute/rocket_vgs_StoreFile?filelabel=' + filename + "&tag=" + tag + "&description=" + filename
		 var reqtime = new Date().getTime()
		axeda.loading(true, reqtime)
		return jQuery.ajax(url, {
			beforeSend: function (xhr) {
				xhr.setRequestHeader('Content-Disposition', filename);
			},
			cache: false,
			processData: false,
			type: 'POST',
			contentType: false,
			data: formData,
			success: function(json){
				$(".ax_loader").remove()
				
				if (callback){
					callback(json)
				}
				return json
			}
		});
		
	}
	
	var scaleImg = function(img, maxWidth, maxHeight){
		var currentwidth = img.width
		var currentheight = img.height
		var ratio
		if (currentwidth > maxWidth || currentheight > maxHeight){
			if ((currentwidth - maxWidth) >= (currentheight - maxHeight)){
				// scale by width
				ratio = maxWidth / currentwidth
				currentwidth = maxWidth
				currentheight = Math.floor(ratio * currentheight)
			}
			else if ((currentheight - maxHeight) > (currentwidth - maxWidth)){
				// scale by height
				ratio = maxHeight / currentheight
				currentwidth = Math.floor(ratio * currentwidth)
				currentheight = maxHeight
			}
			img.width = currentwidth
			img.height = currentheight
		}
	}
	
	var encodeMe = function(text){
		return text.replace(/'/g,"&#39;").replace(/"/g,"&quot;")
	}
	
	var decodeMe = function(text){
		return text.replace(/&#39;/g,"'").replace(/&quot;/g,"\"")
	}


var service = {
    call: function (name, params, returnTo, method) {
		var host = window.location.protocol === 'file:' ? '.' : ""
        var paramString = method == "GET" ? params : JSON.stringify(params);
        if (!method)method = "POST"
        return $.ajax({
            type: method,
            contentType: "application/json; charset=utf-8",
            dataType: "text",
            url: host + "/services/v1/rest/Scripto/execute/" + name,
            data: paramString,
            async: true,
            complete: function (e) {
				if (e.responseText){
					var obj = $.parseJSON(e.responseText)
					return returnTo(obj)
				}
            },
            error: function (e) {
                console.log(e);
            }
        });
    }
}


var ax_widget_line = {
    json: {},
    widget: {},
    chart: {},
	settings: function(){
		$('.ax_widgets .ax_row').eq(3).show()
		$('.ax_widgets .ax_row').eq(4).hide()
		$('.ax_widgets .ax_row').eq(5).hide()
		$('.ax_widgets .ax_row').eq(6).hide()
		$('.ax_widgets .ax_row').eq(7).hide()
	},
    holder: function (num, type) {
        this.json[num] = type
        return '<div id="ax_widget' + num + '" class="ax_widget ax_wide' + type.width + '">' + type.type + '</div>'
    },
    build: function (num, type) {
        this.widget[num] = $('#ax_widget' + num)
        this.widget[num].html('<div class="ax_chart"></div>')
        this.chart[num] = this.widget[num].find('.ax_chart').dxChart({
            dataSource: [
                {pos: 0, val: 0}
            ],
            tooltip: {
                enabled: true
            },
            series: {
                type: 'line',
                argumentField: 'timestamp',
                valueField: 'val',
                color: '#424FFF'
            },
            commonAxisSettings : {
              label : {
                  font: { color: '#ABADA6', size: 15, face: 'Verdana' }
              }
            },
            legend: { visible: false },
            title: type.title 
        })
    },
    run: function (num, data) {
        service.call('rocket_vgs_GetDataItems', {
            "modelNumber": ax_app.appjson.device.modelNumber,
            "serialNumber": ax_app.appjson.device.serialNumber,
            "dataitemName": ax_widget_line.json[num].dataitemName,
            "verbose": "true"
        }, function (e) {
            var data = e.dataItemValues
            if (data) {
                var dataSource = []
                $.each(data, function (k, v) {
                    dataSource.push({timestamp: v.timestamp, val: v.value})
                })

                ax_widget_line.chart[num].dxChart('option', 'animation', false);
                ax_widget_line.chart[num].dxChart('option', 'dataSource', dataSource);
            }
            setTimeout(function () {
                ax_widget_line.run(num, data)
            }, ax_app.refreshrate);
        })
    }
}

var ax_widget_pie = {
    json: {},
    widget: {},
    chart: {},
	settings: function(){
		$('.ax_widgets .ax_row').eq(3).show()
		$('.ax_widgets .ax_row').eq(4).hide()
		$('.ax_widgets .ax_row').eq(5).hide()
		$('.ax_widgets .ax_row').eq(6).hide()
		$('.ax_widgets .ax_row').eq(7).hide()
	},
    holder: function (num, type) {
        this.json[num] = type
        return '<div id="ax_widget' + num + '" class="ax_widget ax_wide' + type.width + '">' + type.type + '</div>'
    },
    build: function (num, type) {
        this.widget[num] = $('#ax_widget' + num)
        this.widget[num].html('<div class="ax_chart"></div>')
        this.chart[num] = this.widget[num].find('.ax_chart').dxPieChart({
            dataSource: [
                {pos: 0, val: 0}
            ],
            series: {
                type: 'pie',
                argumentField: 'pos',
                valueField: 'val'
            },
            legend: { visible: false },
            title: type.title 
        })
        this.run(num)
    },
    run: function (num, data) {
        service.call('rocket_vgs_GetDataItems', {
            "modelNumber": ax_app.appjson.device.modelNumber,
            "serialNumber": ax_app.appjson.device.serialNumber,
            "dataitemName": ax_widget_pie.json[num].dataitemName
        }, function (e) {
            var data = e.dataItemValues
            if (data) {
                var dataSource = []
                $.each(data, function (k, v) {
                    dataSource.push({pos: k, val: v})
                })
                ax_widget_pie.chart[num].dxPieChart('option', 'animation', false);
                ax_widget_pie.chart[num].dxPieChart('option', 'dataSource', dataSource);
            }
            setTimeout(function () {
                ax_widget_pie.run(num, data)
            }, ax_app.refreshrate);
        })
    }
}

var ax_widget_bar = {
    json: {},
    widget: {},
    chart: {},
	settings: function(){
		$('.ax_widgets .ax_row').eq(3).show()
		$('.ax_widgets .ax_row').eq(4).hide()
		$('.ax_widgets .ax_row').eq(5).hide()
		$('.ax_widgets .ax_row').eq(6).hide()
		$('.ax_widgets .ax_row').eq(7).hide()
	},
    holder: function (num, type) {
        this.json[num] = type
        return '<div id="ax_widget' + num + '" class="ax_widget ax_wide' + type.width + '">' + type.type + '</div>'
    },
    build: function (num, type) {
        this.widget[num] = $('#ax_widget' + num)
        this.widget[num].html('<div class="ax_chart"></div>')
        this.chart[num] = this.widget[num].find('.ax_chart').dxChart({
            dataSource: [
                {pos: 0, val: 0}
            ],
            series: {
                type: 'bar',
                argumentField: 'pos',
                valueField: 'val'
            },
            legend: { visible: false },
            title: type.title 
        })
    },
    run: function (num, data) {
        service.call('rocket_vgs_GetDataItems', {
            "modelNumber": ax_app.appjson.device.modelNumber,
            "serialNumber": ax_app.appjson.device.serialNumber,
            "dataitemName": ax_widget_bar.json[num].dataitemName
        }, function (e) {
            var data = e.dataItemValues
            if (data) {
                var dataSource = []
                $.each(data, function (k, v) {
                    dataSource.push({pos: k, val: v})
                })
                ax_widget_bar.chart[num].dxChart('option', 'animation', false);
                ax_widget_bar.chart[num].dxChart('option', 'dataSource', dataSource);
            }
            setTimeout(function () {
                ax_widget_bar.run(num, data)
            }, ax_app.refreshrate);
        })
    }
}

var ax_widget_image = {
    json: {},
    widget: {},
	imgdiv: {},
	img: {},
	settings:  function(){
		$('.ax_widgets .ax_row').eq(3).hide()
		$('.ax_widgets .ax_row').eq(4).hide()
		$('.ax_widgets .ax_row').eq(5).show()
		$('.ax_widgets .ax_row').eq(6).hide()
		$('.ax_widgets .ax_row').eq(7).hide()
		
		$("#ax_widgetimage").off('change.fileinput').on('change.fileinput', function(event){
					if (this.files && this.files.length > 0){
						if (!f.type.match('image.*')) {
							alert("Please select an image file");
							return
						  }
						ax_app.files = this.files
						ax_app.fileinput = this
						var f = this.files[0]
						// Only process image files.
						  
						  var reader = new FileReader();
						// Closure to capture the file information.
						  reader.onload = (function(theFile) {
							return function(e) {
							  var preview = ['<img class="appImage" ', 'src="', e.target.result,
												'" title="', escape(theFile.name), '"/>'].join('');
							  $(".imgpreview").html(preview)
							  var img = $(".imgpreview > img")[0]
							  var dataURL
							  img.onload = function(){
									var maxWidth = 280
									var maxHeight = 280
									scaleImg(img, maxWidth, maxHeight)
									/*
									var canvas = document.createElement("canvas");
									var MAX_WIDTH = 280;
									var MAX_HEIGHT = 280;
									var width = img.width;
									var height = img.height;

									if (width > height) {
									  if (width > MAX_WIDTH) {
										height *= MAX_WIDTH / width;
										width = MAX_WIDTH;
									  }
									} else {
									  if (height > MAX_HEIGHT) {
										width *= MAX_HEIGHT / height;
										height = MAX_HEIGHT;
									  }
									}
									canvas.width = width;
									canvas.height = height;
									var ctx = canvas.getContext("2d");
									ctx.drawImage(img, 0, 0, width, height);

									return canvas.toDataURL(theFile.type);							*/		
								}

							};
						  })(f);
						  
						  // Read in the image file as a data URL.
						  reader.readAsDataURL(f);
					}
				})
				
		
	},
    holder: function (num, type) {
        this.json[num] = type
        return '<div id="ax_widget' + num + '" class="ax_widget ax_wide' + type.width + '">' + type.type + '</div>'
    },
    build: function (num, type) {
        this.widget[num] = $('#ax_widget' + num)
		this.widget[num].html('<span class="title">' + type.title  + '</span><div class="ax_image"></div>')
        this.imgdiv[num] = this.widget[num].find('.ax_image')
		this.img[num] =  new Image()
    },
    run: function (num) {
		service.call('rocket_vgs_GetLastFile', {
			"tag": "*" + ax_app.appjson.device.serialNumber + ",TYPE:IMAGE,*"
		}, function (e) {
			var data = e.url
			
			if (data && ax_widget_image.img[num].src.indexOf(data) == -1){
				ax_widget_image.img[num].src = data
				ax_widget_image.img[num].onload = function(){
					var maxWidth = 325
					var maxHeight = 325
					scaleImg(ax_widget_image.img[num], maxWidth, maxHeight)
					}
				ax_widget_image.imgdiv[num].html(ax_widget_image.img[num])
			}
			
			setTimeout(function () {
				ax_widget_image.run(num, data)
			}, ax_app.refreshrate)
		})
	}
}

var ax_widget_audio = {
    json: {},
    widget: {},
	audiodiv: {},
	audioelement: {},
	buffer: {},
	context: {},
	url:{},
	request:{},
	volume:{},
	gainNode:{},
	source:{},
	playing:{},
	previewfile: {},
	settings:  function(){
		$('.ax_widgets .ax_row').eq(3).hide()
		$('.ax_widgets .ax_row').eq(4).hide()
		$('.ax_widgets .ax_row').eq(5).hide()
		$('.ax_widgets .ax_row').eq(6).show()
		$('.ax_widgets .ax_row').eq(7).hide()
		
		$("#ax_widgetaudio").off('change.fileinput').on('change.fileinput', function(event){
					if (this.files && this.files.length > 0){
						ax_app.files = this.files
						ax_app.fileinput = this
						var f = this.files[0]
						// Only process audio files.
						  if (!f.type.match('audio.*')) {
							alert("Please select an audio file");
							return
						  }
						  var reader = new FileReader();
						// Closure to capture the file information.
						  reader.onload = (function(theFile) {
							return function(e) {
							  var preview = "<audio controls id='audiobtn' ><source src='" + URL.createObjectURL(theFile) + "' type='" + theFile.type + "'></audio>"
							  $(".audiopreview").html(preview)
							};
						  })(f);
						  
						  // Read in the image file as a data URL.
						  reader.readAsDataURL(f);
					}
				})
				
		
	},
    holder: function (num, type) {
        this.json[num] = type
        return '<div id="ax_widget' + num + '" class="ax_widget ax_wide' + type.width + '">' + type.type + '</div>'
    },
    build: function (num, type) {
        this.widget[num] = $('#ax_widget' + num)
		this.widget[num].html("<span class='title'>" + type.title  + "</span><div class='ax_audio'></div>")
        this.audiodiv[num] = this.widget[num].find('.ax_audio')
    },
    run: function (num) {
		service.call('rocket_vgs_GetLastFile', {
			"tag": "" + ax_app.appjson.device.serialNumber + ",TYPE:AUDIO"
		}, function (e) {
			var data = e.url
			if (!ax_widget_audio.url[num] || ax_widget_audio.url[num].indexOf(data) == -1 ||
			(ax_widget_audio.audioelement[num] && ax_widget_audio.audioelement[num].currentTime == ax_widget_audio.audioelement[num].duration)){
				ax_widget_audio.url[num] = data
				var audio = "<audio controls id='audiobtn' ><source src='" + data + "' type='" + e.type + "'></audio>"
				ax_widget_audio.audiodiv[num].html(audio)
				ax_widget_audio.audioelement[num] = ax_widget_audio.audiodiv[num].find("audio")[0]
			}
			
			setTimeout(function () {
				ax_widget_audio.run(num, data)
			}, ax_app.refreshrate*5)
		})
	}
}

var ax_widget_video = {
    json: {},
    widget: {},
	videodiv: {},
	videoelement: {},
	buffer: {},
	context: {},
	url:{},
	request:{},
	volume:{},
	gainNode:{},
	source:{},
	playing:{},
	previewfile: {},
	settings:  function(){
		$('.ax_widgets .ax_row').eq(3).hide()
		$('.ax_widgets .ax_row').eq(4).hide()
		$('.ax_widgets .ax_row').eq(5).hide()
		$('.ax_widgets .ax_row').eq(6).hide()
		$('.ax_widgets .ax_row').eq(7).show()
		
		$("#ax_widgetvideo").off('change.fileinput').on('change.fileinput', function(event){
					if (this.files && this.files.length > 0){
						ax_app.files = this.files
						ax_app.fileinput = this
						var f = this.files[0]
						// Only process video files.
						  if (!f.type.match('video.*')) {
							alert("Please select an video file");
							return
						  }
						  var reader = new FileReader();
						// Closure to capture the file information.
						  reader.onload = (function(theFile) {
							return function(e) {
							  var preview = "<video width='400' height='300' controls id='videobtn' ><source src='" + URL.createObjectURL(theFile) + "' type='" + theFile.type + "'></video>"
							  $(".videopreview").html(preview)
							};
						  })(f);
						  
						  // Read in the image file as a data URL.
						  reader.readAsDataURL(f);
					}
				})
				
		
	},
    holder: function (num, type) {
        this.json[num] = type
        return '<div id="ax_widget' + num + '" class="ax_widget ax_wide' + type.width + '">' + type.type + '</div>'
    },
    build: function (num, type) {
        this.widget[num] = $('#ax_widget' + num)
		this.widget[num].html("<span class='title'>" + type.title  + "</span><div class='ax_video'></div>")
        this.videodiv[num] = this.widget[num].find('.ax_video')

    },
    run: function (num) {
		service.call('rocket_vgs_GetLastFile', {
			"tag": "*" + ax_app.appjson.device.serialNumber + ",TYPE:VIDEO,*"
		}, function (e) {
			var data = e.url

			if (!ax_widget_video.url[num] || ax_widget_video.url[num].indexOf(data) == -1 || 
			(ax_widget_video.videoelement[num] && ax_widget_video.videoelement[num].currentTime == ax_widget_video.videoelement[num].duration)){
				ax_widget_video.url[num] = data
				var video = "<video width='400' height='300' controls id='videobtn' ><source src='" + data + "' type='" + e.type + "'></video>"
				ax_widget_video.videodiv[num].html(video)
				ax_widget_video.videoelement[num] = ax_widget_video.videodiv[num].find("video")[0]
			}
			
			setTimeout(function () {
				ax_widget_video.run(num, data)
			}, ax_app.refreshrate*5)
		})
	}
}


var ax_widget_text = {
    json: {},
    widget: {},
    textdiv: {},
	settings: function(){
		$('.ax_widgets .ax_row').eq(3).show()
		$('.ax_widgets .ax_row').eq(4).hide()
		$('.ax_widgets .ax_row').eq(5).hide()
		$('.ax_widgets .ax_row').eq(6).hide()
		$('.ax_widgets .ax_row').eq(7).hide()
	},
    holder: function (num, type) {
        this.json[num] = type
        return '<div id="ax_widget' + num + '" class="ax_widget ax_wide' + type.width + '">' + type.type + '</div>'
    },
    build: function (num, type) {
        this.widget[num] = $('#ax_widget' + num)
        this.widget[num].html("<span class='title'>" + type.title  + '</span><div class="ax_text"></div>')
        this.textdiv[num] = this.widget[num].find('.ax_text')
		//this.widget[num].css("overflow-y","scroll")
    },
    run: function (num, data) {
        service.call('rocket_vgs_GetDataItems', {
            "modelNumber": ax_app.appjson.device.modelNumber,
            "serialNumber": ax_app.appjson.device.serialNumber,
            "dataitemName": ax_widget_text.json[num].dataitemName,
			"verbose": "true"
        }, function (e) {
            var data = e.dataItemValues
			var html = ""
            if (data) {
				data.reverse()
                $.each(data, function (k, v) {
                    html += "<div class='ax_text_row'>" + v.value + "<span class='ax_text_timestamp'>" + v.timestamp + "</span></div>"
                })
				ax_widget_text.textdiv[num].html(html)

            }
            setTimeout(function () {
                ax_widget_text.run(num, data)
            }, ax_app.refreshrate*2);
        })
    }
}

var ax_widget_alarm = {
    json: {},
    widget: {},
    alarmdiv: {},
	settings: function(){
		$('.ax_widgets .ax_row').eq(3).hide()
		$('.ax_widgets .ax_row').eq(4).hide()
		$('.ax_widgets .ax_row').eq(5).hide()
		$('.ax_widgets .ax_row').eq(6).hide()
		$('.ax_widgets .ax_row').eq(7).hide()
		
	},
    holder: function (num, type) {
        this.json[num] = type
        return '<div id="ax_widget' + num + '" class="ax_widget ax_wide' + type.width + '">' + type.type + '</div>'
    },
    build: function (num, type) {
        this.widget[num] = $('#ax_widget' + num)
        this.widget[num].html("<span class='title'>" + type.title  + '</span><div class="ax_alarm"></div>')
        this.alarmdiv[num] = this.widget[num].find('.ax_alarm')
    },
    run: function (num, data) {
        service.call('rocket_vgs_GetAlarms', {
            "modelNumber": ax_app.appjson.device.modelNumber,
            "serialNumber": ax_app.appjson.device.serialNumber
        }, function (data) {
			var html = ""
			ax_app.debug = data
            if (data) {
				vgsAlarms.processJson(data, '#ax_widget' + num + ' .ax_alarm')

            }
            setTimeout(function () {
                ax_widget_alarm.run(num, data)
            }, ax_app.refreshrate * 3);

        })
    }
}

var ax_widget_map = {
    json: {},
    widget: {},
	settings:  function(){
		$('.ax_widgets .ax_row').eq(3).hide()
		$('.ax_widgets .ax_row').eq(4).hide()
		$('.ax_widgets .ax_row').eq(5).hide()
		$('.ax_widgets .ax_row').eq(6).hide()
		$('.ax_widgets .ax_row').eq(7).hide()
	},
    map: {},
    marker: {},
    holder: function (num, type) {
        this.json[num] = type
        return '<div id="ax_widget' + num + '" class="ax_widget ax_wide' + type.width + '">' + type.type + '</div>'
    },
    build: function (num, type) {
        this.widget[num] = $('#ax_widget' + num)
        this.widget[num].html("<span class='title'>" + type.title + '</span><div class="ax_map"></div>')
        google.maps.visualRefresh = true;
        var center = new google.maps.LatLng(0, 0)
        var mapOptions = {
            center: center,
            zoom: 8
        };

        this.map[num] = new google.maps.Map(this.widget[num].find(".ax_map")[0],
            mapOptions);

    },
    run: function (num, data) {
        service.call('rocket_vgs_GetLocation', {
            "modelNumber": ax_app.appjson.device.modelNumber,
            "serialNumber": ax_app.appjson.device.serialNumber
        }, function (e) {
            var data = e.location
            var lat = data.split(',')[0]
            var lng = data.split(',')[1]
            if (!ax_widget_map.marker[num]) {
                ax_widget_map.marker[num] = new google.maps.Marker({
                    center: new google.maps.LatLng(lat, lng),
                    position: new google.maps.LatLng(lat, lng),
                    map: ax_widget_map.map[num]
                });
            }
            if (ax_widget_map.marker[num].position.pb != lat && ax_widget_map.marker[num].position.qb != lng) {
                ax_widget_map.marker[num].setPosition(new google.maps.LatLng(lat, lng))
                ax_widget_map.map[num].panTo(ax_widget_map.marker[num].getPosition())
            }
            setTimeout(function () {
                ax_widget_map.run(num, data)
            }, ax_app.refreshrate)
        })
    }
}

 var ax_widget_downstreamCMD = {
    json: {},
    widget: {},
	buttondiv:{},
	settings:  function(){
		$('.ax_widgets .ax_row').eq(3).hide()
		$('.ax_widgets .ax_row').eq(4).show()
		$('.ax_widgets .ax_row').eq(5).hide()
		$('.ax_widgets .ax_row').eq(6).hide()
		$('.ax_widgets .ax_row').eq(7).hide()
	},
    holder: function (num, type) {
        //do not edit
        this.json[num] = type
        return '<div id="ax_widget' + num + '" class="ax_widget ax_wide' + type.width + '">' + type.type + '</div>'
    },
    build: function (num, type) {
        // on widget load
        this.widget[num] = $('#ax_widget' + num)      // assign widget to object
        this.widget[num].html("<span class='title'>" + type.title + '</span><div class="ax_downstreamCMD"></div>')
		this.buttondiv[num] = this.widget[num].find('.ax_downstreamCMD')
		var html = ""
		if (type.content){
			var buttons = type.content.split("|")
			$.each(buttons, function(index, el){
				html += "<input type='button' id='btn" + el + "' class='ax_button ax_downstream' value='" + el + "' onclick='ax_app.sendDownstream(&quot;" + el + "&quot;, &quot;btn" + el + "&quot;)'>"
			})
		
		}
		$(this.buttondiv[num]).html(html)
    },
    run: function (num) {

    }
}

/*************************
 * TEMPLATE WIDGET

 // object name should be the widget type prefixed with "ax_widget_"
 var ax_widget_image = {
    json: {},
    widget: {},
    holder: function (num, type) {
        //do not edit
        this.json[num] = type
        return '<div id="ax_widget' + num + '" class="ax_widget ax_wide' + type.width + '">' + type.type + '</div>'
    },
    build: function (num, type) {
        // on widget load
        this.widget[num] = $('#ax_widget' + num)      // assign widget to object
        //your code here
    },
    run: function (num) {
        service.call('groovyscriptname', {
            "modelNumber": ax_app.appjson.device.modelNumber,
            "serialNumber": ax_app.appjson.device.serialNumber
        }, function (e) {
            var data = e.location
            // update using response
            //your code here

            // reload run at interval
            setTimeout(function () {
                ax_widget_map.run(num, data)
            }, ax_app.refreshrate)
        })
        //if non refreshing remove service call.
    }
}
 */






