# Reactive debounce demo

This example demonstrates how reactive programming can be used to filter incoming events in a web interface if too many events are received. 

Various strategies are showcased here. 

* In one of the strategies, the first element that arrives at the backend is let through, and then a timer is started for five seconds to filter out further elements. Each additional incoming element restarts this time span. 
* There's a second implementation as well. In this approach, incoming events are initially held back until no further elements arrive for a span of five seconds. Only then is the last arrived element passed through.
