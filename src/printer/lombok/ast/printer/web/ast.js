
(function() {
	$(function() {
		parenHighlighting();
		makeTree();
	});
	
	function parenHighlighting() {
		$(".open, .clos").hover(function(evt) {
			var me = $(evt.currentTarget);
			var i = me.attr("id");
			if (!i) return;
			i = i.substring(5);
			$("#clos_" + i + ", #open_" + i).toggleClass("outlined");
		});
	}
	
	function makeTree() {
		var ctr = 0;
		var currentHighlight = $();
		
		function dehighlightTreeItem() {
			if (currentHighlight.size() > 0) {
				currentHighlight.removeClass("highlight");
				currentHighlight = $();
			}
		}
		
		function makeTreeElement(elem) {
			var e = $(elem);
			var kind = e.attr("kind");
			var desc = e.attr("description");
			
			if (!kind) {
				var passon = $();
				e.children().each(function() {
					var child = makeTreeElement(this);
					$(child).each(function() {
						passon = passon.add(this);
					});
				});
				return passon;
			}
			
			var txt = e.attr("relation");
			txt = txt ? (txt + ": ") : "";
			txt += kind;
			if (desc) txt += " " + desc;
			ctr++;
			var out = $("<div>").addClass("expanded").attr("id", "treeNode" + ctr);
			out.append($("<span>").addClass("treeNodeDescription").text(txt).hover(function (evt) {
					e.toggleClass("highlight");
			}));
			e.data("treeKey", "treeNode" + ctr);
			var arrowNeeded = false;
			e.children().each(function() {
				var c = makeTreeElement(this);
				if (c) {
					if ($(c).size() > 0) arrowNeeded = true;
					out.append(c);
				}
			});
			if (arrowNeeded) {
				out.prepend($("<span>").addClass("treeNodeArrow").click(function(evt) {
					$(this).parent().toggleClass("expanded").toggleClass("collapsed");
					evt.preventDefault()}));
			} else {
				out.prepend($("<span>").addClass("treeNodeArrow"));
				out.addClass("leaf");
			}
			return out;
		}
		
		$("#source>.Node").each(function() {
			var elem = makeTreeElement(this);
			if (elem) $("#tree").append(elem);
		});
		
		$("#source .Node").mouseover(function(evt) {
			var key = evt.currentTarget ? $(evt.currentTarget).data("treeKey") : "";
			if (!key) return;
			evt.stopPropagation();
			var node = key ? $("#" + key) : $();
			if (node.size() == 0) {
				dehighlightTreeItem();
				return;
			}
			if (currentHighlight.size() > 0 && currentHighlight.first() == node.first()) return;
			dehighlightTreeItem();
			currentHighlight = node;
			currentHighlight.addClass("highlight");
		}).dblclick(function(evt) {
			var key = evt.currentTarget ? $(evt.currentTarget).data("treeKey") : "";
			if (key) {
				var c = $("#tree");
				var t = $("#" + key);
				t.parents(".collapsed").removeClass("collapsed").addClass("expanded");
				var s = t.offset().top - c.offset().top;
				c.animate({scrollTop: "+=" + s + "px"}, 250);
				evt.stopPropagation();
			}
		});
	}
})();

