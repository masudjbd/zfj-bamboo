var _fdfdff3443343 = "";
AJS.$(document).ready(function() {
	var actionTD = '<td class="action" headers="action"><a href="#actions" aria-owns="actions" aria-haspopup="true" class="aui-button aui-style-default aui-dropdown2-trigger">     <span class="aui-icon aui-icon-small aui-iconfont-configure"></span> </a><div id="actions" class="aui-style-default aui-dropdown2">    <ul class="aui-list-truncate"><li><a href="#" class="deleteConfig">Delete</a></li>    </ul></div></td>';
  var baseUrl = AJS.$("meta[name='application-base-url']").attr("content");
    
  function populateForm() {
    AJS.$.ajax({
    	cache: false,
      url: baseUrl + "/rest/zfj-admin/1.0/",
      dataType: "json",
      success: function(config) {
    	  if (config == undefined) {
    		  return;
    	  }
    	  
    	  var serversTableData = "";
    	  var serverCount = config.length;
    	  for (var serverCounter = 0; serverCounter < serverCount; serverCounter++) {
			var server = config[serverCounter];
			var tableRow = "";
			tableRow += "<tr>";
			var tableData = "";
			tableData += "<td class='deleteConfig" + serverCounter + " editConfig"  + serverCounter + "'>" + server.serverAddr + "</td>";
			tableData += "<td>" + server.user + "</td>";
			tableData += "<td><span class='aui-icon aui-icon-small aui-iconfont-success' style='color:green;'></span></td>";
			tableData += '<td class="action" headers="action"><a href="#actions' + serverCounter + '" aria-owns="actions' + serverCounter + '" aria-haspopup="true" class="aui-button aui-style-default aui-dropdown2-trigger">     <span class="aui-icon aui-icon-small aui-iconfont-configure"></span> </a><div id="actions' + serverCounter + '" class="aui-style-default aui-dropdown2">    <ul class="aui-list-truncate"><li><a href="#" class="deleteConfig" id="deleteConfig' + serverCounter + '">Delete</a></li>  <li><a href="#" class="editConfig" id="editConfig' + serverCounter + '">Edit</a></li>  </ul></div></td>';
			
			tableRow += tableData;
			tableRow += "</tr>";
			
			serversTableData += tableRow
		}
    	  AJS.$("table.aui tbody").empty();
    	  AJS.$("table.aui tbody").append(serversTableData);
    	  attachDeleteConfigEvent();
    	  attachEditConfigEvent();
      }
    });
  }
  populateForm();

});