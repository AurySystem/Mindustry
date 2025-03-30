package io.anuke.mindustry.world.blocks.types.distribution;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerBlock;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static io.anuke.mindustry.Vars.syncBlockState;

public class Teleporter extends PowerBlock{

	protected int capacity = 180;
	private Array<Tile> removal = new Array<>();
	private Array<Tile> returns = new Array<>();

	protected float powerPerItem = 0.12f;

	public Teleporter(String name) {
		super(name);
		update = true;
		solid = true;
		health = 80;
		powerCapacity = 40f;
		instantTransfer = true;
	}

	@Override
	public void configure(Tile tile, byte data) {
		TeleporterEntity entity = tile.entity();
		if(entity != null){
			entity.connectID = data;
			Arrays.fill(entity.items, 0);
		}
	}

	@Override
	public boolean isConfigurable(Tile tile) {return true;}

	@Override
	public void getStats(Array<String> list){
		super.getStats(list);
		list.add("[powerinfo]Power/item: " + Strings.toFixed(powerPerItem, 1));
	}

	@Override
	public void placed(Tile tile){
		tile.<TeleporterEntity>entity().connectID = lastConnected;
		setConfigure(tile, lastConnected);

	}
	
	@Override
	public void draw(Tile tile){
		super.draw(tile);

		Draw.color(Color.WHITE);
		Draw.alpha(0.45f + Mathf.absin(Timers.time(), 7f, 0.26f));
		Draw.rect("blank", tile.worldx(), tile.worldy(), 2 ,2);
		Draw.reset();
	}
	
	@Override
	public void update(Tile tile){
		TeleporterEntity entity = tile.entity();

		teleporters[entity.connectID].add(tile);

		if(entity.totalItems() > 0) {
			tryDump(tile);
		}

	}

	@Override
	public void tapped(Tile tile) {
	}

	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		TeleporterEntity entity = tile.entity();
		return !(source.block() instanceof Teleporter) && entity.getItem(item) < capacity;
	}

	@Override
	public void handleItem(Item item, Tile tile, Tile source){
		TeleporterEntity entity = tile.entity();
		entity.addItem(item, 1);
	}

	@Override
	public TileEntity getEntity(){
		return new TeleporterEntity();
	}
	
	private Array<Tile> findLinks(Tile tile){
		TeleporterEntity entity = tile.entity();
		
		removal.clear();
		returns.clear();
		
		for(Tile other : teleporters[entity.connectID]){
			if(other != tile){
				if(other.block() instanceof Teleporter){
					if(other.<TeleporterEntity>entity().connectID != entity.connectID){
						removal.add(other);
					}else if(other.entity.totalItems() == 0){
						returns.add(other);
					}
				}else{
					removal.add(other);
				}
			}
		}

		for(Tile remove : removal)
			teleporters[entity.connectID].remove(remove);
		
		return returns;
	}

	public static class TeleporterEntity extends PowerEntity{
		public byte connectID = 00;


		@Override
		public void write(DataOutputStream stream) throws IOException{
			stream.writeByte(connectID);
		}

		@Override
		public void read(DataInputStream stream) throws IOException{
			connectID = stream.readByte();
		}

		}

	}

}
