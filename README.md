This is a slimmed-down, slightly-optimized fork of Lottery made for MineCave (minecave.com). I am attempting to keep the open-source magic of this project going, but I will not offer any support for this plugin. Feel free to compile it yourself and use it like the upstream branch as you please.

Removed Features:
=
* Items as a replacement for money
* Eseentials dependency (only Vault is needed)
* Tax sent to an account
* Debug mode
* Claiming (no longer necessary as offline players will get their money reward, and material rewards are removed)
* Opting out of lottery messages/broadcasts (/lottery messages)
* In-game config editing (/lottery config)
* Unnecessary comma removal (which led to minor language bug)

Changed Features:
=
* 1 ticket per line stored in a long .txt file (can cause lag when a lot of tickets are purchased) --> tickets stored in YAML file with format "player uuid:ticket count"
* Broadcast every ticket purchase (configurable) --> broadcast only when someone buys the maximum amount of tickets

Added Features:
=
* Caching winners to reduce file reading
* Broadcasting lottery status every x (configurable) minutes (see "config.broadcastInterval" in config.yml file)