# Home-Assistant-Call-notification
Catch Android Call event and push it to Home Assistant

Simple App to catch Android Call events and publish them to home assistant.

To use it, simple fill all needed information :

-Your HA url without the "/" at the end
-Your long token
-Press test and the press start service.

When you reboot your phone, the service restart automatically.

To catch event in HA, you can use NodeRed with this simple setup (use the import function) :

[{"id":"11932e.46c044d3","type":"tab","label":"Flow 2","disabled":false,"info":""},{"id":"a1c55124.f6109","type":"server-events","z":"11932e.46c044d3","name":"call_notif","server":"2e34e2f3.46ee6e","event_type":"call_notif","exposeToHomeAssistant":false,"haConfig":[{"property":"name","value":""},{"property":"icon","value":""}],"x":100,"y":100,"wires":[["4c0beb30.808dfc"]]},{"id":"4c0beb30.808dfc","type":"switch","z":"11932e.46c044d3","name":"","property":"payload.event.type","propertyType":"msg","rules":[{"t":"eq","v":"onIncomingCallStarted","vt":"str"},{"t":"eq","v":"onMissedCall","vt":"str"}],"checkall":"true","repair":false,"outputs":2,"x":230,"y":100,"wires":[["6c69915c.628a18"],["6c69915c.628a18"]]},{"id":"6c69915c.628a18","type":"debug","z":"11932e.46c044d3","name":"","active":true,"tosidebar":true,"console":false,"tostatus":false,"complete":"false","x":470,"y":100,"wires":[]},{"id":"2e34e2f3.46ee6e","type":"server","z":"","name":"Home Assistant","legacy":false,"addon":false,"rejectUnauthorizedCerts":true,"ha_boolean":"y|yes|true|on|home|open","connectionDelay":true,"cacheJson":true}]

The payload contains information about the caller, your device and event type.
Available events :
- onIncomingCallStarted
- onOutgoingCallStarted
- onIncomingCallEnded
- onOutgoingCallEnded
- onMissedCall

