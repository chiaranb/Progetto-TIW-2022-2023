{
	//page components
	let categoriesForm = null;
	let categoriesList = null;
	let tree = null;
	let alertContainer = document.getElementById("id_alert");
	let pageOrchestrator = new PageOrchestrator(); // main controller

	//Prepare thw view after the load is complete
	window.addEventListener("load", () => {
		if (sessionStorage.getItem("username") == null) {
			window.location.href = "Login.html";
		} else {
			pageOrchestrator.start();
			pageOrchestrator.refresh();
		}
	}, false);

	//Component that shows the pesonal message
	function PersonalMessage(_username, messagecontainer) {
		this.username = _username;
		this.show = function() {
			messagecontainer.textContent = this.username;
		}
	};

	//Component that handles the creation of the tree	
	function TreeList() {

		this.show = function() {
			var self = this;
			makeCall("GET", "GetTree", null,
				function(req) {
					if (req.readyState == XMLHttpRequest.DONE) {
						var message = req.responseText;
						if (req.status == 200) {
							var tree = JSON.parse(req.responseText);
							if (tree.length == 0) {
								alertContainer.textContent = "No categories yet!";
								return;
							}
							self.update(tree);
						} else if (req.status == 403) {
							window.location.href = req.getResponseHeader("Location");
							window.sessionStorage.removeItem('username');
						} else {
							//request failed, handle it
							alertContainer.textContent = message
						}
					}
				});
		};

		this.update = function(treeList) {
			var container = document.getElementById("id_container");
			container.innerHTML = "";

			treeList.forEach(function(category) {
				container.appendChild(createTree(category));
			});
			//Create drop here root
			if (treeList[0].subCategories.length != 9) {
				var dropSpan = document.createElement("span");
				dropSpan.textContent = "drop here";
				dropSpan.className = "drophere";
				dropSpan.style.display = "none";
				dropSpan.dataset.id = "0";
				container.appendChild(dropSpan);
			}
		};

		this.registerEvents = function(orchestrator) {
			initializeDragAndDrop(orchestrator);
		};
	};

	//Component that creates the structure of the tree	
	function createTree(category) {
		var ul = document.createElement("ul");
		ul.classList.add("subcategory");

		category.subCategories.forEach(function(child) {
			var li = document.createElement("li");

			var span = document.createElement("span");
			span.textContent = child.position + ' ' + child.name;
			span.classList.add("name");
			span.dataset.id = child.id;

			//Event Listener double click
			span.addEventListener('dblclick', () => {
				doubleClick(span);
			}, false);

			var childList = createTree(child);

			//Drag element
			var drag = document.createElement("span");
			drag.textContent = " " + "drag me";
			drag.classList.add("dragme");
			drag.setAttribute('draggable', 'true');

			li.appendChild(span);
			li.appendChild(drag);

			//Drop element
			var drop = document.createElement("span");
			if (child.subCategories.length != 9) {
				drop.textContent = " " + "drop here";
				drop.className = "drophere";
				drop.style.display = "none";
			}
			li.appendChild(drop);
			li.appendChild(childList);
			ul.appendChild(li);
		});
		return ul;
	};

	//Component that add the copied subtree 
	function addSubtree(subtree) {
		var root = subtree[0];
		var parent = document.createDocumentFragment();
		var li = document.createElement("li");
		var span = document.createElement("span");

		span.textContent = "~" + ' ' + root.name;
		span.classList.add("subtree");
		li.appendChild(span);
		parent.appendChild(li);

		li.appendChild(createSubtree(root));
		return li;
	};

	//Component that create the structure of the copied subtree
	function createSubtree(category) {
		var ul = document.createElement("ul");

		category.subCategories.forEach(function(child) {
			var li = document.createElement("li");
			var span = document.createElement("span");
			span.textContent = "~" + ' ' + child.name;
			span.classList.add("subtree");

			var childList = createSubtree(child);

			li.appendChild(span);
			li.appendChild(childList);
			ul.appendChild(li);
		});
		return ul;
	};

	//Double click for modify the name
	function doubleClick(span) {
		var currentName = span.textContent.split(' ').slice(1).join(' ');
		var input = document.createElement("input");
		input.type = "text";
		input.value = currentName;

		input.addEventListener('blur', () => {
			var newName = input.value;
			var form = document.createElement("form");
			var inputName = document.createElement("input");
			inputName.type = "hidden";
			inputName.name = "name";
			inputName.value = newName;
			form.appendChild(inputName);
			var inputId = document.createElement("input");
			inputId.type = "hidden";
			inputId.name = "id";
			inputId.value = input.nextElementSibling.dataset.id;
			form.appendChild(inputId);
			var previousPos = span.textContent.split(' ')[0];

			//Save the new category name in DB
			makeCall("POST", "ModifyName", form,
				function(req) {
					if (req.readyState == XMLHttpRequest.DONE) {
						var message = req.responseText;
						if (req.status == 200) {
							var spanElem = document.createElement("span");
							spanElem.textContent = previousPos + ' ' + newName;
							span.parentNode.replaceChild(spanElem, span);
							tree.show();
							categoriesList.show()
						} else if (req.status == 403) {
							window.location.href = req.getResponseHeader("Location");
							window.sessionStorage.removeItem('username');
						}
						else {
							//request failed, handle it
							alertContainer.textContent = message;
						}
					}
				});
			span.style.display = 'inline';
			//Toglie l'input
			input.parentNode.removeChild(input);
		}, false);

		span.style.display = 'none';
		span.parentNode.insertBefore(input, span);
		input.focus();
	};


	//Component that shows all categories for the form
	function CategoriesList() {

		this.show = function() {
			var self = this;
			makeCall("GET", "GetCategoriesData", null,
				function(req) {
					if (req.readyState == XMLHttpRequest.DONE) {
						var message = req.responseText;
						if (req.status == 200) {
							let categories = JSON.parse(req.responseText);
							self.update(categories);
						} else if (req.status == 403) {
							window.location.href = "Login.html";
							window.sessionStorage.removeItem('username');
						} else {
							//request failed, handle it
							alertContainer.textContent = message
						}
					}
				});
		};

		this.update = function(listCategories) {
			var selectElement = document.getElementById("father");
			selectElement.innerHTML = "";

			for (let i = 1; i < listCategories.length; i++) {
				let p = listCategories[i];
				if (p.subCategories.length !== 9) {
					let option = document.createElement("option");
					option.value = listCategories[i].id;
					option.textContent = listCategories[i].position + " " + listCategories[i].name;
					selectElement.appendChild(option);
				}
			}
		}
	};

	//Component that handles the form to create a new category
	function CategoryForm(formCategories) {
		this.formCategories = formCategories;

		this.registerEvents = function(orchestrator) {
			this.formCategories.querySelector("input[type='button']").addEventListener('click',
				(e) => {
					var form = e.target.closest("form");
					if (form.checkValidity()) {
						makeCall("POST", "CreateCategory", form,
							function(req) {
								if (req.readyState == XMLHttpRequest.DONE) {
									var message = req.responseText;
									if (req.status == 200) {
										orchestrator.refresh();
									} else if (req.status == 403) {
										window.location.href = "Login.html";
										window.sessionStorage.removeItem('username');
									} else {
										//request failed, handle it
										alertContainer.textContent = message;
									}
								}
							});
					} else {
						form.reportValidity();
					}
				}, false);

			this.formCategories.addEventListener("keydown", (e) => {
				if (e.key === "Enter") {
					e.preventDefault();
				}
			}, false);

		};
	};

	//Hide the drop here for the child
	function hideDropHereElements(element) {
		var drophereSpan = element.querySelector("span.drophere");
		if (drophereSpan) {
			drophereSpan.style.visibility = "hidden";
		}

		var children = element.children;
		for (let i = 0; i < children.length; i++) {
			var child = children[i];
			hideDropHereElements(child);
		}
	};

	//Drag and drop event
	function initializeDragAndDrop(orchestrator) {
		var self = this;
		var dragElements = document.getElementsByClassName("dragme");
		var dropElements = document.getElementsByClassName("drophere");
		var dragElement;

		//Drag start
		document.addEventListener("dragstart", (e) => {
			dragElement = e.target.previousElementSibling;

			for (let i = 0; i < dragElements.length; i++) {
				var target = dragElements[i];

				let spanRoot = target.parentElement.parentElement;
				if (spanRoot.nextElementSibling) {
					spanRoot.nextElementSibling.style.display = "inline";
				}
				if (e.target != target) {
					//Hide dragme 
					target.style.display = "none";
					//Show drop here
					target.nextElementSibling.style.display = "inline";
				}
				else {
					target.classList.add("dragging");
					let children = target.nextElementSibling.nextElementSibling.children;
					for (let i = 0; i < children.length; i++) {
						var child = children[i];
						hideDropHereElements(child);
					}
				}
			}
		}, false);

		//Drag over
		document.addEventListener("dragover", (e) => {
			e.preventDefault();
			var dest = e.target.closest(".drophere");
			if (dest && dest.matches(".drophere")) {
				dest.classList.add("selected");
				dest.classList.remove("nonselected");
			}
		}, false);

		//Drag leave
		document.addEventListener("dragleave", (e) => {
			var dest = e.target.closest(".drophere");
			if (dest && dest.matches(".drophere")) {
				dest.classList.remove("selected");
				dest.classList.add("nonselected");
			}
		}, false);

		//Drop
		document.addEventListener("drop", (e) => {
			e.preventDefault();
			var dropElement = e.target.closest(".drophere");

			if (dropElement != null) {
				var dropId = dropElement.previousElementSibling.previousElementSibling != null
					? dropElement.previousElementSibling.previousElementSibling.dataset.id
					: 0;

				//Drag end
				document.addEventListener("dragend", () => {
					for (let i = 0; i < dragElements.length; i++) {
						var target = dragElements[i];
						target.style.display = "none";
					}
				}, false);

				categoriesForm = document.getElementById("id_categoryform");

				var confirmationText = "Do you to want confirm or cancel the select?";
				if (window.confirm(confirmationText)) {
					categoriesForm.style.display = "none";
					dropElement.style.display = "none";

					for (let i = 0; i < dropElements.length; i++) {
						var drophere = dropElements[i];
						if (e.target != drophere) {
							drophere.classList.remove("selected", "nonselected");
							drophere.style.display = "none";
						}
					}

					makeCall("GET", "Copy?categoryid=" + dragElement.dataset.id, null,
						function(req) {
							if (req.readyState == XMLHttpRequest.DONE) {
								var message = req.responseText;
								if (req.status == 200) {
									var subtree = JSON.parse(req.responseText);
									if (subtree.length == 0) {
										return;
									}
									self.addSubtree(subtree);
								} else if (req.status == 403) {
									window.location.href = req.getResponseHeader("Location");
									window.sessionStorage.removeItem('username');
								} else {
									//request failed, handle it
									alertContainer.textContent = message;
									orchestrator.refresh();
									categoriesForm.style.display = "inline";
								}
							}
						});
				} else {
					dropElement = null;
					orchestrator.refresh();
				}
			} else {
				orchestrator.refresh();
			}

			this.addSubtree = function(subtree) {
				var container = document.getElementById("id_container");
				let targetNode = dropId == 0
					? container.childNodes[0]
					: dropElement.nextElementSibling;

				targetNode.appendChild(addSubtree(subtree));

				//Save button
				var saveButton = document.createElement("button");
				saveButton.className = "save-button";
				saveButton.textContent = "Save";
				document.body.appendChild(saveButton);
				saveButton.addEventListener("click", () => {
					saveButton.remove();

					dropId = dropElement.previousElementSibling.previousElementSibling != null
						? dropElement.previousElementSibling.previousElementSibling.dataset.id
						: 0;

					makeCall("GET", "CopyHere?fatherid=" + dropId + "&id=" + dragElement.dataset.id, null,
						function(req) {
							if (req.readyState == XMLHttpRequest.DONE) {
								var message = req.responseText;
								if (req.status == 200) {
									orchestrator.refresh();
								} else if (req.status == 403) {
									window.location.href = req.getResponseHeader("Location");
									window.sessionStorage.removeItem('username');
								} else {
									//request failed, handle it
									alertContainer.textContent = message;
								}
							}
						});
					dropElement = null;
					categoriesForm.style.display = "inline";
					orchestrator.refresh();
				}, false);
			}
		}, false);
	};

	function PageOrchestrator() {

		this.start = function() {
			let personalMessage = new PersonalMessage(sessionStorage.getItem('username'),
				document.getElementById("id_username"));
			personalMessage.show();

			categoriesList = new CategoriesList();
			categoriesList.show();

			categoriesForm = new CategoryForm(document.getElementById("id_categoryform"));
			categoriesForm.registerEvents(this);

			tree = new TreeList();
			tree.show(this);
			tree.registerEvents(this);

			document.querySelector("a[href='Logout']").addEventListener('click', () => {
				window.sessionStorage.removeItem('username');
			});
		};

		this.refresh = function() {
			categoriesList.show();
			tree.show();
		};
	};
}