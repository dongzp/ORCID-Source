<#--

    =============================================================================

    ORCID (R) Open Source
    http://orcid.org

    Copyright (c) 2012-2013 ORCID, Inc.
    Licensed under an MIT-Style License (MIT)
    http://orcid.org/open-source-license

    This copyright and license information (including a link to the full license)
    shall be included in its entirety in all copies or substantial portion of
    the software.

    =============================================================================

-->
<@public >

<script type="text/ng-template" id="view-details-modal">
	<div style="padding:20px">
		<a id="cboxClose" class="btn pull-right close-button" ng-click="closeColorBox()">X</a>
		<h1><@orcid.msg 'manage_clients.client_information'/></h1>
		<table class="table table-bordered">
			<tr>
				<td><@orcid.msg 'manage_clients.client_id'/></td>
				<td>{{clientDetails.clientId.value}}</td>
			</tr>
			<tr>
				<td><@orcid.msg 'manage_clients.client_secret'/></td>
				<td>{{clientDetails.clientSecret.value}}</td>
			</tr>
		</table>		
	</div>
</script>

<script type="text/ng-template" id="edit-client-modal">
	<div style="padding: 20px;">
		<a id="cboxClose" class="btn pull-right close-button" ng-click="closeColorBox()">X</a>
		<h1><@orcid.msg 'manage_clients.edit_client'/></h1>			
		<form class="form-horizontal">
			<div class="control-group">
				<label class="control-label" for="clientname" style="margin-right:10px; text-align:left; width:90px"><@orcid.msg 'manage_clients.display_name'/>: </label>
		    	<div class="relative">
					<input id="clientname" type="text" ng-model="clientToEdit.displayName.value" required />
					<span class="orcid-error" ng-show="clientToEdit.displayName.errors.length > 0">
						<div ng-repeat='error in clientToEdit.displayName.errors' ng-bind-html-unsafe="error"></div>
					</span>
				</div>
		    </div>
		    <div class="control-group">
				<label class="control-label" for="website" style="margin-right:10px; text-align:left; width:90px"><@orcid.msg 'manage_clients.website'/>: </label>
		    	<div class="relative">
					<input id="website" type="text" ng-model="clientToEdit.website.value" required />
					<span class="orcid-error" ng-show="clientToEdit.website.errors.length > 0">
						<div ng-repeat='error in clientToEdit.website.errors' ng-bind-html-unsafe="error"></div>
					</span>
				</div>	
		    </div>
		    <div class="control-group">
				<label class="control-label" for="description" style="margin-right:10px; text-align:left; width:90px"><@orcid.msg 'manage_clients.description'/>: </label>
		    	<div class="relative">
					<input id="description" type="text" ng-model="clientToEdit.shortDescription.value" required />
					<span class="orcid-error" ng-show="clientToEdit.shortDescription.errors.length > 0">
						<div ng-repeat='error in clientToEdit.shortDescription.errors' ng-bind-html-unsafe="error"></div>
					</span>
				</div>	
		    </div>		    	    	    	
	    	<div ng-show="clientToEdit.redirectUris.length">
	    		<div id="edit-client-table">
		    		<div class="control-group" ng-repeat='rUri in clientToEdit.redirectUris'>						
						<label class="control-label" style="margin-right:10px; text-align:left; width:90px"><@orcid.msg 'manage_clients.redirect_uri'/>:</label>
						<div class="relative">
							<input type="text" placeholder="<@orcid.msg 'manage_clients.redirect_uri_placeholder'/>"  ng-model="rUri.value.value">						
							<a href ng-click="deleteUri($index)" class="glyphicon glyphicon-trash blue"></a>
							<span class="orcid-error" ng-show="rUri.errors.length > 0">
								<div ng-repeat='error in rUri.errors' ng-bind-html-unsafe="error"></div>
							</span>
						</div>	
					</div>
		    	</div>
	    	</div>
	    	<div ng-show="!clientToEdit.redirectUris.redirectUri.length">			
				<a href ng-click="addUriToExistingClientTable()" class="icon-plus-sign blue"><@orcid.msg 'manage_clients.add_redirect_uri'/></a>
			</div>
			<div class="controls save-btns pull-right bottom-margin-small">
				<span id="bottom-submit-update-credential-request" ng-click="submitEditClient($index)" class="btn btn-primary"><@orcid.msg 'manage_clients.update'/></span>				
			</div> 	    	
		</form>		
    <div> 
</script>

<script type="text/ng-template" id="new-client-modal">
	<div style="padding: 20px;">
		<a id="cboxClose" class="btn pull-right close-button" ng-click="closeColorBox()">X</a>
		<h1><@orcid.msg 'manage_clients.add_new'/></h1>
		
		<form class="form-horizontal">
			<div class="control-group">
				<label class="control-label" for="clientname" style="margin-right:10px; text-align:left; width:90px"><@orcid.msg 'manage_clients.display_name'/>: </label>
				<div class="relative">
					<input id="clientname" type="text" placeholder="<@orcid.msg 'manage_clients.display_name_placeholder'/>" class="input-xlarge" ng-model="newClient.displayName.value" required />
					<span class="orcid-error" ng-show="newClient.displayName.errors.length > 0">
						<div ng-repeat='error in newClient.displayName.errors' ng-bind-html-unsafe="error"></div>
					</span>
				</div>								
			</div>									
		 	<div class="control-group">
		 		<label class="control-label" for="website" style="margin-right:10px; text-align:left; width:90px"><@orcid.msg 'manage_clients.website'/>: </label>
				<div class="relative">
		 			<input id="website" type="text" placeholder="<@orcid.msg 'manage_clients.website_placeholder'/>" class="input-xlarge" ng-model="newClient.website.value" required />
					<span class="orcid-error" ng-show="newClient.website.errors.length > 0">
						<div ng-repeat='error in newClient.website.errors' ng-bind-html-unsafe="error"></div>
					</span>
				</div>	 		
		 	</div>
			<div class="control-group">
				<label class="control-label" for="description" style="margin-right:10px; text-align:left; width:90px"><@orcid.msg 'manage_clients.description'/>: </label>
		    	<div class="relative">
					<input id="description" type="text" placeholder="<@orcid.msg 'manage_clients.description_placeholder'/>" class="input-xlarge" ng-model="newClient.shortDescription.value" required />
					<span class="orcid-error" ng-show="newClient.shortDescription.errors.length > 0">
						<div ng-repeat='error in newClient.shortDescription.errors' ng-bind-html-unsafe="error"></div>
					</span>
				</div>	
			</div>			
	    	<div id="new-client-table">
		    	<div class="control-group" ng-repeat='rUri in newClient.redirectUris'>						
					<label class="control-label" style="margin-right:10px; text-align:left; width:90px"><@orcid.msg 'manage_clients.redirect_uri'/>:</label>
					<div class="relative">
						<input type="text" placeholder="<@orcid.msg 'manage_clients.redirect_uri_placeholder'/>" class="input-xlarge" ng-model="rUri.value.value">
						<span class="orcid-error" ng-show="rUri.errors.length > 0">
							<div ng-repeat='error in rUri.errors' ng-bind-html-unsafe="error"></div>
						</span>						
					</div>
				</div>
		    </div>
		</form>
		<div ng-show="!clientToEdit.redirectUris.redirectUri.length">			
			<a href ng-click="addUriToNewClientTable()" class="icon-plus-sign blue"><@orcid.msg 'manage_clients.add_redirect_uri'/></a>
		</div>
		<div class="controls save-btns pull-left bottom-margin-small">
			<span id="bottom-submit-credential-request" ng-click="submitAddClient()" class="btn btn-primary"><@orcid.msg 'manage_clients.submit'/></span>				
		</div>
	</div>
</script>


<div class="row">
	<div class="span3 lhs override">
		<ul class="settings-nav">
			<li><a href="<@spring.url '/account' />#account-settings"><@orcid.msg 'manage.accountsettings'/></a></li>
			<li><a href="<@spring.url '/account' />#manage-permissions"><@orcid.msg 'manage.managepermission'/></a></li>
		</ul>
	</div>
	<div class="span9">			
		<div ng-controller="ClientEditCtrl" class="clients">			
			<div ng-show="!clients.length" ng-cloak>
				<span><@orcid.msg 'manage_clients.no_clients'/></span>
			</div>							
			
			<div ng-hide="!clients.length" ng-cloak>				
					<div class="bottom-margin-small" ng-repeat='client in clients'>
						<div class="pull-right"><a href="#" ng-click="viewDetails($index)" class="icon-zoom-in blue"></a></div>
						<div class="pull-right"><a href="#" ng-click="editClient($index)" class="icon-pencil  blue"></a></div>
						<div>							
							<h4><@orcid.msg 'manage_clients.client_id'/>: {{client.clientId.value}}</h4>
							<ul>
								<li><span><@orcid.msg 'manage_clients.display_name'/></span>: {{client.displayName.value}}</li>
								<li><span><@orcid.msg 'manage_clients.website'/>:</span> <a href="{{client.website}}" target="_blank">{{client.website.value}}</a></li>
								<li><span><@orcid.msg 'manage_clients.description'/>:</span> {{client.shortDescription.value}}</li>
								<li>
									<div ng-repeat='rUri in client.redirectUris'>			                	
					                	<span><@orcid.msg 'manage_clients.redirect_uri'/></span>: <a href="{{rUri.value.value}}" target="_blank">{{rUri.value.value}}</a>
									</div>
								</li>
							</ul>
		                </div>
					</div>
			</div>
			
			<@security.authorize ifAnyGranted="ROLE_PREMIUM_INSTITUTION, ROLE_PREMIUM, ROLE_ADMIN">
				<div class="controls save-btns pull-left">
					<span id="bottom-create-new-client-premium" ng-click="addClient()" class="btn btn-primary"><@orcid.msg 'manage_clients.add'/></span>				
				</div>
			</@security.authorize>
			<@security.authorize ifAnyGranted="ROLE_BASIC_INSTITUTION, ROLE_BASIC">
				<#if (group)?? && (group.orcidClient)?? && !(group.orcidClient?has_content)> 
					<div class="controls save-btns pull-left" ng-show="!clients.length">
						<span id="bottom-create-new-client" ng-click="addClient()" class="btn btn-primary"><@orcid.msg 'manage_clients.add'/></span>				
					</div>
				</#if>
			</@security.authorize>
		</div>						
	</div>
</div>
</@public >
