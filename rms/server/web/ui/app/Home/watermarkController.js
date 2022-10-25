mainApp.controller('watermarkController',[ '$scope', '$state', 'userPreferenceService', '$timeout',
	function($scope, $state, userPreferenceService, $timeout){
		$scope.hideTagLabel = false;
		$scope.watermarkTags = {
			"Date": false, "Time": false, "EmailID": false
		}
		$scope.watermarkTagComponents = {"$(Date)": "Date", "$(Time)": "Time", "$(User)": "EmailID", "$(Break)": "Line Break"}
		$scope.watermarkMaxLen = 50;
		$scope.isLoading = true;
		var init = function() {
			$scope.inUserPreference = $state.current.name === STATE_USER_PREFERENCE;
			if ($scope.watermarkStr) {
				$timeout(function () {
					convertWatermarkStrToTags({results:{watermark:$scope.watermarkStr}});
				}, 100);
				$scope.isLoading = false;
			} else {
				userPreferenceService.getPreference(function (data) {
					$scope.isLoading = false;
					if (data.statusCode == 200) {
						if (data.results != undefined) {
							if (data.results.watermark) {
								convertWatermarkStrToTags(data);
							}
						}
					} else {
						var messages = $filter('translate')('user.preference.load.fail');
						var isSuccess = false;
						showSnackbar({
							isSuccess: isSuccess,
							messages: messages
						});
					}
				});
			}
		}

		var convertWatermarkStrToTags = function(data) {
			var watermark = data.results.watermark;
			var splitStrings = watermark.split(/(\$\([^()]+\))/);
			splitStrings.map(function(x) {
				return x.replace(/^\$\((.*)\)$/, '$$$1');
			}).filter(Boolean)
			for (var i in splitStrings ){
				if(getWatermarkTagComponent(splitStrings[i])){
					if(splitStrings[i] == "$(Break)") {
						$scope.addNewLine(getWatermarkTagComponent(splitStrings[i]), true);
					} else {
						$scope.addTags(getWatermarkTagComponent(splitStrings[i]), true);
					}					
				} else {
					var encodedStr = htmlEntityEncode(splitStrings[i]);
					$('div#watermark-div').last().append(encodedStr);			
				}
			}
		}

		var getWatermarkTagComponent = function (tagKey){
			return $scope.watermarkTagComponents[tagKey];
		}

		var getWatermarkTagValue = function(tagValue) {
			for(var key in $scope.watermarkTagComponents) {
				if($scope.watermarkTagComponents[key] === tagValue) {
					return key;
				}
			}
		}

		$scope.showWatermarkResult = function(watermarkStr) {
			if(watermarkStr == undefined) {
				return;
			}
			$('span.watermarkResult').empty();
			var splitStrings = watermarkStr.split(/(\$\([^()]+\))/);
			splitStrings.map(function(x) {
				return x.replace(/^\$\((.*)\)$/, '$$$1');
			}).filter(Boolean)
			for (var i in splitStrings ){
				var tagInnerHTML = getWatermarkTagComponent(splitStrings[i]);
				if(tagInnerHTML){
					if(splitStrings[i] == "$(Break)") {
						var tag = document.createElement("span");
						tag.innerHTML = tagInnerHTML;
						tag.contentEditable = false;
						tag.className = "watermark-newLine";
						tag.style.padding = "0 3px";
						tag.style.margin = "0";
						$('span.watermarkResult').append(tag);
					} else {
						var tag = document.createElement("span");
						tag.className = "watermark-tags";
						tag.style.padding = "0 3px";
						tag.style.margin = "0";
						tag.innerHTML = tagInnerHTML;
						tag.contentEditable = false;
						$('span.watermarkResult').append(tag);
					}					
				} else {
					var encodedStr = htmlEntityEncode(splitStrings[i]);
					$('span.watermarkResult').append(encodedStr);			
				}
			}
		}

		$scope.addTags = function(tags, onload) {
			$scope.watermarkTags[tags] = true;
			var tag = document.createElement("span");
			tag.innerHTML = tags;
			tag.contentEditable = false;
			tag.id = "watermark-tags";
			tag.className = "watermark-tags";
			insertTagToDOM(tag, onload);
		}

		$scope.addNewLine = function(tags, onload) {
			var tag = document.createElement("span");
			tag.innerHTML = tags;
			tag.contentEditable = false;
			tag.id = "watermark-newLine";
			tag.className = "watermark-newLine";
			insertTagToDOM(tag, onload);
		}

		var insertTagToDOM = function(tag, onload) {
			var anchorNode = document.getSelection().anchorNode;
			if (anchorNode && anchorNode.parentNode == $('div#watermark-div').last()[0]) {
				$('div#watermark-div').last()[0].insertBefore(document.createTextNode('\u200C'),anchorNode.nextSibling);
				$('div#watermark-div').last()[0].insertBefore(tag, anchorNode.nextSibling);
				$('div#watermark-div').last()[0].insertBefore(document.createTextNode('\u200C'),anchorNode.nextSibling);
			} else {
				$('div#watermark-div').last().append(document.createTextNode('\u200C'));
				$('div#watermark-div').last().append(tag);
				$('div#watermark-div').last().append(document.createTextNode('\u200C'));
			}
			if (!onload) {
				setCursorPosition(tag);
			}
		}

		var setCursorPosition = function(ele) {
			var r = document.createRange();
			r.setStart(ele, 1);
			r.setEnd(ele,1);
			var s = window.getSelection();
			s.removeAllRanges();
			s.addRange(r);
		}

		var updateHideLabel = function(){
			for (var tags in $scope.watermarkTags) {
				if($scope.watermarkTags[tags] == false) {
					$scope.hideTagLabel = false;
					break;
				}
				$scope.hideTagLabel = true;
			}
		}

		var updateTags = function (){
			for (var tags in $scope.watermarkTags) {
					$scope.watermarkTags[tags] = false;
			}
			NodeList.prototype.forEach = Array.prototype.forEach
			var children =  $('div#watermark-div:last')[0].childNodes;
			children.forEach(function(item){
				if (item.id == "watermark-tags"){
					 $scope.watermarkTags[item.textContent] = true;
					 updateHideLabel();
				}
			});
		}

		$scope.keypress = function(event) {
			if (event.keyCode === 10 || event.keyCode === 13) 
				event.preventDefault();
		}

		$scope.addWatermarkStr = function() {
			var watermarkStr = "";
			NodeList.prototype.forEach = Array.prototype.forEach
			if ($('div#watermark-div:last')[0]) {
				var children =  $('div#watermark-div:last')[0].childNodes;
				children.forEach(function(item){
				    if (item.id == "watermark-tags" || item.id == "watermark-newLine"){
						watermarkStr = watermarkStr + getWatermarkTagValue(item.textContent) ;
					} else {
						watermarkStr = watermarkStr + item.textContent;
						watermarkStr = watermarkStr.replace('\u200C','');
					}
				});
			}
			return watermarkStr;
		}

		$scope.$watch(function () {
			if ($('div#watermark-div:last')[0]) {
				return $('div#watermark-div:last')[0].innerHTML;
			}
		}, function(val) {
			var waterMark = $scope.addWatermarkStr();
			$scope.watermarkLength = waterMark.length;
			updateHideLabel();
			if($scope.watermarkLoaded) {
				$scope.$emit('onWatermarkChanged', {error: $scope.watermarkLength == 0 || $scope.watermarkLength > $scope.watermarkMaxLen});
			}
			if($scope.watermarkLength > 0) {
				$scope.watermarkLoaded = true;
			}
		});

		$(document).on('click', '#watermark-tags', function(event)
		{
			$(this).remove();
			NodeList.prototype.forEach = Array.prototype.forEach
			var children =  $('div#watermark-div:last')[0].childNodes;
			children.forEach(function(item){
				if (item.textContent == "\u200C" && item.nextSibling !=null && item.nextSibling.textContent == "\u200C"){
					item.parentNode.removeChild(item);
				}
			});
			updateTags();
			$scope.$apply();
		});

		$(document).on('click', '#watermark-newLine', function(event)
		{
			$(this).remove();
			$scope.$apply();
		});

		$scope.keyup = function(event) {
			// Check for a backspace
			if (event.which == 8 || event.which == 46) {
				if (jscd.browser == "Microsoft Internet Explorer") {
					/*
					var s = window.getSelection();
					var r = s.getRangeAt(0)
					var el = r.startContainer.parentNode
					// Check if the current element is the tag or new Line
					if (el.id == 'watermark-tags' || 'watermark-newLine') {
					// Check if we are exactly at the end of the .label element
						if (r.startOffset == r.endOffset && r.endOffset == el.textContent.length) {
							event.preventDefault();
							// remove the element
							el.parentNode.removeChild(el);
						}
					}
					*/
				} else if (jscd.browser == "Safari") { //fix bug 46826
					$('br', 'div#watermark-div:last').remove();
				}
				updateTags();
			} else if (jscd.browser == "Microsoft Edge") {
				var node = document.querySelector("div#watermark-div");
				node.focus();
				var textNode = node.lastChild;
				var caret = textNode.data.length; 
				var range = document.createRange();
				range.setStart(textNode, caret);
				range.setEnd(textNode, caret);
				var sel = window.getSelection();
				sel.removeAllRanges();
				sel.addRange(range);	
			}
		}

		init();
	}
]);
