[#if serverValid == "success" || serverValid == "No Projects" ]

[@ww.select labelKey="zephyr.task.server" name="serverAddress" required="true" list="serverMap" listKey="key" listValue="key" toggle=true /]
[@ww.select labelKey="zephyr.task.project" name="projectKey" required="true" list="projectMap" listKey="key" listValue="value" toggle=true /]
[@ww.select labelKey="zephyr.task.release" name="releaseKey" required="true" list="releaseMap" listKey="key" listValue="value" toggle=true /]
[@ww.select labelKey="zephyr.task.cycle" name="cycleKey" required="true" list="cycleMap" listKey="key" listValue="value" toggle=true /]
[@ww.select labelKey="zephyr.task.cycleDuration" name="cycleDuration" required="true" list="cycleDurationMap" listKey="key" listValue="value" toggle=true /]
[@ww.textfield labelKey="zephyr.task.cycleNamePrefix" name="cyclePrefix" required='false'/]

[#else]
<div class="buttons-container">
        <div class="buttons">
<label>${serverValid} : </lablel>
<a href="${baseUrl}/plugins/servlet/zfj/admin">Add/Edit Zephyr Servers</a>
        </div>
    </div>
[/#if]
<script type="text/javascript">
AJS.$(document).ready(function () {

	function populateCyclesAjax() {
		AJS.$.ajax({
			cache: false,
			url: BAMBOO.contextPath + "/rest/zfj-admin/1.0/cycle?serverAddress=" + AJS.$("#serverAddress").val() + "&projectKey=" + AJS.$("#projectKey").val() + "&releaseKey=" + AJS.$("#releaseKey").val(),
			dataType: "json",
			success: function (cycles) {
				var options = "";
				for (var key in cycles) {
					if (cycles.hasOwnProperty(key)) {
						var option = '<option value="' + key + '">' + cycles[key] + '</option>';
						options += option;
					}
				}

				AJS.$("#cycleKey").html(options);
			}
		});

	}

	function populateProjectsAjax() {

		AJS.$.ajax({
			cache: false,
			url: BAMBOO.contextPath + "/rest/zfj-admin/1.0/project?serverAddress=" + AJS.$("#serverAddress").val(),
			dataType: "json",
			success: function (projects) {
				var options = "";
				for (var key in projects) {
					if (projects.hasOwnProperty(key)) {
						var option = '<option value="' + key + '">' + projects[key] + '</option>';
						options += option;
					}
				}
				AJS.$("#projectKey").html(options);
				populateReleasesAjax();
			}
		});

	}

	function populateReleasesAjax() {

		AJS.$.ajax({
			cache: false,
			url: BAMBOO.contextPath + "/rest/zfj-admin/1.0/release?serverAddress=" + AJS.$("#serverAddress").val() + "&projectKey=" + AJS.$("#projectKey").val(),
			dataType: "json",
			success: function (releases) {
				var options = "";
				for (var key in releases) {
					if (releases.hasOwnProperty(key)) {
						var option = '<option value="' + key + '">' + releases[key] + '</option>';
						options += option;
					}
				}
				AJS.$("#releaseKey").html(options);
				populateCyclesAjax();
			}
		});

		AJS.$.ajax({
			cache: false,
			url: BAMBOO.contextPath + "/rest/zfj-admin/1.0/cycleDuration?serverAddress=" + AJS.$("#serverAddress").val() + "&projectKey=" + AJS.$("#projectKey").val(),
			dataType: "json",
			success: function (projectDurations) {
				var options = "";
				for (var key in projectDurations) {
					if (projectDurations.hasOwnProperty(key)) {
						var option = '<option value="' + key + '">' + projectDurations[key] + '</option>';
						options += option;
					}
				}
				AJS.$("#cycleDuration").html(options);
			}
		});
	}

	function registerListeners() {
		AJS.$("#serverAddress").on("change", function() {populateProjectsAjax();});
		AJS.$("#projectKey").on("change", function() {populateReleasesAjax();});
		AJS.$("#releaseKey").on("change", function() {populateCyclesAjax();});

		if (AJS.$("#serverAddress").val() != undefined && AJS.$("#serverAddress").val().trim() != "" && (AJS.$("#projectKey").val() == undefined || AJS.$("#projectKey").val().trim() == "")) {
			populateProjectsAjax();
		}
	}
	registerListeners();
});

</script>