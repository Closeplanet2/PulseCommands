<h2 align="center">
<br>
<img src="Images/Logo.png" alt="Panda Spigot Core" width="600">
<br>
<br>
A Bukkit/Spigot API to allow for the ease and accesibility of spigot creation!
<br>
</h2>

```
<repository>
      <id>pulsecommands</id>
      <url>https://maven.pkg.github.com/closeplanet2/PulseCommands</url>
</repository>
```

```
<dependency>
      <groupId>com.pandapulsestudios</groupId>
      <artifactId>pulsecommands</artifactId>
      <version>1.1.10-a</version>
</dependency>
```

<h1 align="center"> Auto Register Commands </h1>

```
@Override
public void onEnable() {
      ...
      PulseCommands.RegisterRaw(this);
      ...
}
```

<h1 align="center"> PlayerCommand </h1>

```
@PCCommand
public class TestCommand extends PlayerCommand{
    public TestCommand() { super("commandName", false, "cn"); }

    @PCPerm({"perm1", "perm2"})
    @PCRegion({"region1", "region2"})
    @PCWorld({"world1", "world2"})


    @PCMethod
    @PCOP
    @PCSignature("sig1.sig2")
    public void TestCommandFunction(UUID playerUUID, boolean state){
        var player = Bukkit.getPlayer(playerUUID);
        if(player == null) return;
        player.sendMessage("Boolean: " + state);
    }

    @PCMethod
    @PCOP
    @PCSignature("sig1.sig2")
    public void TestCommandFunction(UUID playerUUID, int number){
        var player = Bukkit.getPlayer(playerUUID);
        if(player == null) return;
        player.sendMessage("Number: " + number);
    }

    @PCMethod
    @PCOP
    @PCSignature("sig1.sig2")
    @PCTab(pos = 0, type = TabType.Information_From_Function, data = "Names")
    public void TestCommandFunction(UUID playerUUID, String answer){
        var player = Bukkit.getPlayer(playerUUID);
        if(player == null) return;
        player.sendMessage("String: " + answer);
    }

    @PCMethodData
    public List<String> Names(String currentInput){
        return Arrays.asList(new String[]{"a", "b", "c", "d", "e"});
    }

}
```

<h1 align="center"> Interfaces </h1>

```
@PCCommand
@PCFunctionHideTab
@PCHideTab
@PCMethod
@PCMethodData
@PCOP
@PCPerm({"perm1", "perm2"})
@PCRegion({"region1", "region2"})
@PCSignature
@PCTab
@PCWorld({"world1", "world2"})
```
