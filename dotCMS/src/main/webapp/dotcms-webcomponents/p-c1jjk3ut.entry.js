import{r as t,c as i,h as e,g as n,H as s}from"./p-17c4ed33.js";import{c as o,a as r}from"./p-cbaf9c4f.js";var a=o((function(t){t.exports=function(){function t(t,i){for(var e=0;e<i.length;e++){var n=i[e];n.enumerable=n.enumerable||!1,n.configurable=!0,"value"in n&&(n.writable=!0),Object.defineProperty(t,n.key,n)}}var i=function(t){return t.innerHTML=""},e=function(t,e,n,s,o,r){s({event:t,query:e instanceof HTMLInputElement?e.value:e.innerHTML,matches:o.matches,results:o.list.map((function(t){return t.value})),selection:o.list.find((function(i){return 13===t.keyCode?i.index===Number(r.getAttribute("data-id")):"mousedown"===t.type?i.index===Number(t.currentTarget.getAttribute("data-id")):void 0}))}),i(n)},n=function(t){return"string"==typeof t?document.querySelector(t):t()},s=function(t){return"<span class=".concat("autoComplete_highlighted",">").concat(t,"</span>")},o=i,r=function(t,i){i=i||{bubbles:!1,cancelable:!1,detail:void 0};var e=document.createEvent("CustomEvent");return e.initCustomEvent(t,i.bubbles,i.cancelable,i.detail),e};r.prototype=window.Event.prototype;var a={CustomEventWrapper:"function"==typeof window.CustomEvent&&window.CustomEvent||r,initElementClosestPolyfill:function(){Element.prototype.matches||(Element.prototype.matches=Element.prototype.msMatchesSelector||Element.prototype.webkitMatchesSelector),Element.prototype.closest||(Element.prototype.closest=function(t){var i=this;do{if(i.matches(t))return i;i=i.parentElement||i.parentNode}while(null!==i&&1===i.nodeType);return null})}};return function(){function i(t){!function(t,i){if(!(t instanceof i))throw new TypeError("Cannot call a class as a function")}(this,i);var e,s,o=t.selector,r=void 0===o?"#autoComplete":o,a=t.data,u=a.key,h=a.src,c=a.cache,l=void 0===c||c,d=t.query,f=t.trigger,v=(f=void 0===f?{}:f).event,m=void 0===v?["input"]:v,p=f.condition,w=void 0!==p&&p,y=t.searchEngine,E=void 0===y?"strict":y,g=t.threshold,b=void 0===g?0:g,C=t.debounce,T=void 0===C?0:C,L=t.resultsList,_=(L=void 0===L?{}:L).render,D=void 0!==_&&_,R=L.container,k=L.position,x=void 0===k?"afterend":k,H=L.element,$=void 0===H?"ul":H,I=L.navigation,M=void 0!==I&&I,P=t.sort,O=void 0!==P&&P,A=t.placeHolder,S=t.maxResults,j=void 0===S?5:S,q=t.resultItem,B=(q=void 0===q?{}:q).content,K=void 0!==B&&B,N=q.element,X=void 0===N?"li":N,Y=t.noResults,F=t.highlight,W=void 0!==F&&F,z=t.onSelection,G=D?(e={container:void 0!==R&&R,destination:L.destination||n(r),position:x,element:$},(s=document.createElement(e.element)).setAttribute("id","autoComplete_list"),e.container&&e.container(s),e.destination.insertAdjacentElement(e.position,s),s):null;this.selector=r,this.data={src:function(){return"function"==typeof h?h():h},key:u,cache:l},this.query=d,this.trigger={event:m,condition:w},this.searchEngine="loose"===E?"loose":"function"==typeof E?E:"strict",this.threshold=b,this.debounce=T,this.resultsList={render:D,view:G,navigation:M},this.sort=O,this.placeHolder=A,this.maxResults=j,this.resultItem={content:K,element:X},this.noResults=Y,this.highlight=W,this.onSelection=z,this.init()}var r,u;return r=i,(u=[{key:"search",value:function(t,i){var e=i.toLowerCase();if("loose"===this.searchEngine){t=t.replace(/ /g,"");for(var n=[],o=0,r=0;r<e.length;r++){var a=i[r];o<t.length&&e[r]===t[o]&&(a=this.highlight?s(a):a,o++),n.push(a)}return o===t.length&&n.join("")}if(e.includes(t))return t=new RegExp("".concat(t),"i").exec(i),this.highlight?i.replace(t,s(t)):i}},{key:"listMatchedResults",value:function(t){var i=this;return new Promise((function(e){var n=[];t.filter((function(t,e){var s=function(s){var o=s?t[s]:t;if(o){var r="function"==typeof i.searchEngine?i.searchEngine(i.queryValue,o):i.search(i.queryValue,o);r&&s?n.push({key:s,index:e,match:r,value:t}):r&&!s&&n.push({index:e,match:r,value:t})}};if(i.data.key){var o=!0,r=!1,a=void 0;try{for(var u,h=i.data.key[Symbol.iterator]();!(o=(u=h.next()).done);o=!0)s(u.value)}catch(c){r=!0,a=c}finally{try{o||null==h.return||h.return()}finally{if(r)throw a}}}else s()}));var s=i.sort?n.sort(i.sort).slice(0,i.maxResults):n.slice(0,i.maxResults);return e({matches:n.length,list:s})}))}},{key:"ignite",value:function(){var t=this,i=n(this.selector);this.placeHolder&&i.setAttribute("placeholder",this.placeHolder);var s=function(n){Promise.resolve(t.data.cache?t.dataStream:t.data.src()).then((function(s){t.dataStream=s,function(n){var s=i instanceof HTMLInputElement||i instanceof HTMLTextAreaElement?i.value.toLowerCase():i.innerHTML.toLowerCase(),r=t.queryValue=t.query&&t.query.manipulate?t.query.manipulate(s):s,u=t.resultsList.render,h=t.trigger.condition?t.trigger.condition(r):r.length>t.threshold&&r.replace(/ /g,"").length,c=function(t,e){i.dispatchEvent(new a.CustomEventWrapper("autoComplete",{bubbles:!0,detail:{event:t,input:s,query:r,matches:e?e.matches:null,results:e?e.list:null},cancelable:!0}))};if(u){var l=t.resultsList.view;o(l),h?t.listMatchedResults(t.dataStream,n).then((function(s){c(n,s),t.resultsList.render&&(0===s.list.length&&t.noResults?t.noResults():(function(t,i,e){var n=document.createDocumentFragment();i.forEach((function(t,s){var o=document.createElement(e.element);o.setAttribute("data-id",i[s].index),o.setAttribute("class","autoComplete_result"),e.content?e.content(t,o):o.innerHTML=t.match||t,n.appendChild(o)})),t.appendChild(n)}(l,s.list,t.resultItem),t.onSelection&&(t.resultsList.navigation?t.resultsList.navigation(n,i,l,t.onSelection,s):function(t,i,n,s){var o,r=i.childNodes,a=r.length-1,u=void 0,h=function(t){u.classList.remove("autoComplete_selected"),o=1===t?u.nextSibling:u.previousSibling},c=function(t){(u=t).classList.add("autoComplete_selected")};t.onkeydown=function(l){if(r.length>0)switch(l.keyCode){case 38:u?(h(0),c(o||r[a])):c(r[a]);break;case 40:u?(h(1),c(o||r[0])):c(r[0]);break;case 13:u&&e(l,t,i,n,s,u)}},r.forEach((function(o){o.onmousedown=function(o){return e(o,t,i,n,s)}}))}(i,l,t.onSelection,s))))})):c(n)}else!u&&h&&t.listMatchedResults(t.dataStream,n).then((function(t){c(n,t)}))}(n)}))};this.trigger.event.forEach((function(e){var n,o,r;i.addEventListener(e,(n=function(t){return s(t)},o=t.debounce,function(){var t=this,i=arguments;clearTimeout(r),r=setTimeout((function(){return n.apply(t,i)}),o)}))}))}},{key:"init",value:function(){var t=this;this.data.cache?Promise.resolve(this.data.src()).then((function(i){t.dataStream=i,t.ignite()})):this.ignite(),a.initElementClosestPolyfill()}}])&&t(r.prototype,u),i}()}()}));const u=class{constructor(e){t(this,e),this.disabled=!1,this.placeholder="",this.threshold=0,this.maxResults=0,this.debounce=300,this.data=null,this.id=`autoComplete${(new Date).getTime()}`,this.keyEvent={Enter:this.emitEnter.bind(this),Escape:this.clean.bind(this)},this.selection=i(this,"selection",7),this.enter=i(this,"enter",7),this.lostFocus=i(this,"lostFocus",7)}componentDidLoad(){this.data&&this.initAutocomplete()}render(){return e("input",{autoComplete:"off",disabled:this.disabled||null,id:this.id,onBlur:t=>this.handleBlur(t),onKeyDown:t=>this.handleKeyDown(t),placeholder:this.placeholder||null})}watchThreshold(){this.initAutocomplete()}watchData(){this.initAutocomplete()}watchMaxResults(){this.initAutocomplete()}handleKeyDown(t){const{value:i}=this.getInputElement();i&&this.keyEvent[t.key]&&(t.preventDefault(),this.keyEvent[t.key](i))}handleBlur(t){t.preventDefault(),setTimeout(()=>{document.activeElement.parentElement!==this.getResultList()&&(this.clean(),this.lostFocus.emit(t))},0)}clean(){this.getInputElement().value="",this.cleanOptions()}cleanOptions(){this.getResultList().innerHTML=""}emitselect(t){this.clean(),this.selection.emit(t)}emitEnter(t){t&&(this.clean(),this.enter.emit(t))}getInputElement(){return this.el.querySelector(`#${this.id}`)}initAutocomplete(){this.clearList(),new a({data:{src:async()=>this.getData()},sort:(t,i)=>t.match<i.match?-1:t.match>i.match?1:0,placeHolder:this.placeholder,selector:`#${this.id}`,threshold:this.threshold,searchEngine:"strict",highlight:!0,maxResults:this.maxResults,debounce:this.debounce,resultsList:{container:()=>this.getResultListId(),destination:this.getInputElement(),position:"afterend"},resultItem:({match:t})=>t,onSelection:({event:t,selection:i})=>{t.preventDefault(),this.focusOnInput(),this.emitselect(i.value)}})}clearList(){const t=this.getResultList();t&&t.remove()}focusOnInput(){this.getInputElement().focus()}getResultList(){return this.el.querySelector(`#${this.getResultListId()}`)}getResultListId(){return`${this.id}_results_list`}async getData(){const t=this.getInputElement();t.setAttribute("placeholder","Loading...");const i="function"==typeof this.data?await this.data():[];return t.setAttribute("placeholder",this.placeholder||""),i}get el(){return n(this)}static get watchers(){return{threshold:["watchThreshold"],data:["watchData"],maxResults:["watchMaxResults"]}}static get style(){return"dot-autocomplete input{-webkit-box-sizing:border-box;box-sizing:border-box;width:200px}dot-autocomplete ul{background-color:#fff;list-style:none;margin:0;max-height:300px;overflow:auto;padding:0;position:absolute;width:200px}dot-autocomplete ul li{background-color:#fff;border-top:0;border:1px solid #ccc;-webkit-box-sizing:border-box;box-sizing:border-box;cursor:pointer;padding:.25rem}dot-autocomplete ul li:first-child{border-top:1px solid #ccc}dot-autocomplete ul li:focus{background-color:#ffffe0;outline:0}dot-autocomplete ul li .autoComplete_highlighted{font-weight:700}"}},h=class{constructor(e){t(this,e),this.label="",this.deleteLabel="Delete",this.disabled=!1,this.remove=i(this,"remove",7)}render(){const t=this.label?`${this.deleteLabel} ${this.label}`:null;return e(s,null,e("span",null,this.label),e("button",{type:"button","aria-label":t,disabled:this.disabled,onClick:()=>this.remove.emit(this.label)},this.deleteLabel))}get el(){return n(this)}static get style(){return"dot-chip span{margin-right:.25rem}dot-chip button{cursor:pointer}"}};export{u as dot_autocomplete,h as dot_chip};