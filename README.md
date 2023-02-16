KubeJS EEX adds the following entity events for KubeJS:
 - an entity has been moved to a different world.
 - a player has been moved to a different world.
 - an entity is directly responsible for killing another entity.
 - check if elytra flight (both through normal and custom elytras) is allowed.
 
```js
console.info('Hello, World! (You will see this line every time server resources reload)')

EntityEvents.worldChanged(event => console.log(event.origin))
PlayerEvents.worldChanged(event => console.log(event.origin))
EntityEvents.killedOtherEntity(event => console.log(event.killedEntity))
EntityEvents.allowElytraFlight(event => event.cancel())
```