AJS.$(document).ready(function() {

	attachTestConfigEvent();
	attachConfigSaveEvent();
	
});

function attachTestConfigEvent() {
	AJS.$("#server-validate-button").on("click", function() {
		validateServerInfo();
	});
}


function attachDeleteConfigEvent() {
	AJS.$("a.deleteConfig").on("click", function() {
		deleteServerInfo(this);
	});
}

function attachEditConfigEvent() {
	AJS.$("a.editConfig").on("click", function() {
		editServerInfo(this);
	});
}

function editServerInfo(zephyrServerEditbutton) {
	var baseUrl = AJS.$("meta[name='application-base-url']").attr("content");
	var serverName = AJS.$('td.' + AJS.$(zephyrServerEditbutton).attr('id')).text();

	AJS.$("#server-save-button").off("click");
	attachSaveEditedConfigEvent(serverName);
    AJS.$.ajax({
    	cache: false,
      url: baseUrl + "/rest/zfj-admin/1.0/serverAddr?serverAddr=" + serverName,
      dataType: "json",
      success: function(config) {
    	  if (config == undefined) {
    		  return;
    	  }
			var server = config[0];
			var serverAddr = server.serverAddr
			var user = server.user
			var pass = server.pass
			
            AJS.$('form.aui').show();
            AJS.$('form.aui').find("input").clearInputs();
       		AJS.$("div.zephyr-form-error").hide();
			AJS.$("div.zephyr-form-success").hide();
			AJS.$("div.zephyr-form-validating").hide();

            
            AJS.$("input#url").val(serverAddr);
            AJS.$("input#username").val(user);
            AJS.$("input#password").val(pass);
      }
    });
  
}

function deleteServerInfo(zephyrServerDeletebutton) {
	var baseUrl = AJS.$("meta[name='application-base-url']").attr("content");

	var serverName = AJS.$('td.' + AJS.$(zephyrServerDeletebutton).attr('id')).text();
	var serverObj = {};
	serverObj.serverAddr = serverName;
	
    AJS.$.ajax({
    	cache: false,
	      url: baseUrl + "/rest/zfj-admin/1.0/",
	      type: "DELETE",
	      contentType: "application/json",
	      data: JSON.stringify(serverObj),
	      success: function(validationMsg) {
	    	     location.reload();
	      }
	    });

}

function validateServerInfo(configSaveEdit, PreviousServerAddr) {
	
		AJS.$("div.zephyr-form-error").hide();
		AJS.$("div.zephyr-form-success").hide();
		AJS.$("div.zephyr-form-validating").show();

		var serverAddr = AJS.$('input#url').val();
		var user = AJS.$('#username').val();
		var pass = AJS.$('#password').val();

		console.log(serverAddr);
		console.log(user);
		console.log(pass);
		var zephyrServers = [];
		var zephyrServer = {};
		zephyrServer.serverAddr = serverAddr;
		zephyrServer.user = user;
		zephyrServer.pass = pass;
	
		zephyrServers.push(zephyrServer);

		var baseUrl = AJS.$("meta[name='application-base-url']").attr("content");

		    AJS.$.ajax({
		    	cache: false,
		      url: baseUrl + "/rest/zfj-admin/1.0/CredCheck",
		      type: "POST",
		      contentType: "application/json",
		      data: JSON.stringify(zephyrServers),
		      success: function(validationMsg) {
		    	  
		    	  AJS.$("div.zephyr-form-validating").hide();
		    	  if(validationMsg.statusMsg != "Connection to Zephyr has been validated") {
		    		  AJS.$("div.zephyr-form-error").text(validationMsg.statusMsg).show();
		    		  } else {
		    			  if (configSaveEdit != undefined) {
		    				  configSave(configSaveEdit, PreviousServerAddr);
		    			  } else {
		    				  AJS.$("div.zephyr-form-success").text(validationMsg.statusMsg).show();
		    			  }
		    		  } 
		      }
		    });
}
function attachConfigSaveEvent() {
	AJS.$("#server-save-button").on("click", function() {
		validateServerInfo("save");
	});

}

function attachSaveEditedConfigEvent(PreviousServerAddr) {
	AJS.$("#server-save-button").on("click", function() {
		validateServerInfo("edit", PreviousServerAddr);
	});

}

function configSave(configSaveEdit, PreviousServerAddr) {
	var baseUrl = AJS.$("meta[name='application-base-url']").attr("content");
	var url = "";

	var zephyrServers = [];

	var serverAddr = AJS.$("input#url").val();
	var user = AJS.$("#username").val();
	var pass = AJS.$("#password").val();

	var zephyrServer = {};

	if (configSaveEdit != undefined && configSaveEdit == "edit") {
		zephyrServer.serverAddr = PreviousServerAddr + "::" + serverAddr;
		url = baseUrl + "/rest/zfj-admin/1.0/edit";
	} else {
		zephyrServer.serverAddr = serverAddr;
		url = baseUrl + "/rest/zfj-admin/1.0/"
	}
	zephyrServer.user = user;
	zephyrServer.pass = pass;
	zephyrServers.push(zephyrServer);

	AJS.$.ajax({
		url: url,
		cache: false,
		type: "PUT",
		contentType: "application/json",
		data: JSON.stringify(zephyrServers),
		success: function(validationMsg) {
			location.reload();
		},
		processData: false
	});

}