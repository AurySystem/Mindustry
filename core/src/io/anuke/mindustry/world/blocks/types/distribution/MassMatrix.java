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

public class MassMatrix extends PowerBlock{
	protected final int timerSend = timers++;
	public static final Color[] colorArray = {Color.ROYAL, Color.ORANGE, Color.SCARLET, Color.FOREST,
			Color.PURPLE, Color.GOLD, Color.WHITE, Color.BLACK, Color.GRAY, Color.CYAN, Color.CHARTREUSE, Color.PINK};
	public static final int colors = colorArray.length;

	private static ObjectSet<Tile>[] teleporters = new ObjectSet[colors];
	private static byte lastColor = 0;

	protected int capacity = 180;
	private Array<Tile> removal = new Array<>();
	private Array<Tile> returns = new Array<>();

	protected float powerPerItem = 0.12f;

	static{
		for(int i = 0; i < colors; i ++){
			teleporters[i] = new ObjectSet<>();
		}
	}
	
	public MassMatrix(String name) {
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
			entity.color = data;
			Arrays.fill(entity.items, 0);
		}
	}

	@Override
	public void getStats(Array<String> list){
		super.getStats(list);
		list.add("[powerinfo]Power/item: " + Strings.toFixed(powerPerItem, 1));
	}

	@Override
	public void placed(Tile tile){
		tile.<TeleporterEntity>entity().color = lastColor;
		setConfigure(tile, lastColor);
	}
	
	@Override
	public void draw(Tile tile){
		TeleporterEntity entity = tile.entity();
		
		super.draw(tile);
		
		Draw.color(colorArray[entity.color]);
		Draw.rect("teleporter-top", tile.worldx(), tile.worldy(), 24, 24);
		Draw.color(Color.WHITE);
		Draw.alpha(0.45f + Mathf.absin(Timers.time(), 7f, 0.26f));
		Draw.rect("blank", tile.worldx(), tile.worldy(), 2 ,2);
		Draw.reset();
	}
	
	@Override
	public void update(Tile tile){
		TeleporterEntity entity = tile.entity();

		teleporters[entity.color].add(tile);

		if(entity.totalItems() > 0) {
			tryDump(tile);
		}

		if(entity.timer.get(timerSend, 80)) {
			sendItems(tile);
		}
	}

	@Override
	public boolean isConfigurable(Tile tile){
		return true;
	}
	
	@Override
	public void buildTable(Tile tile, Table table){
		TeleporterEntity entity = tile.entity();

		ButtonGroup<ImageButton> group = new ButtonGroup<>();
		Table cont = new Table();
		cont.margin(4);
		cont.marginBottom(5);

		cont.add().colspan(4).height(105f);
		cont.row();

		for(int i = 0; i < colors; i ++){
			final int f = i;
			ImageButton button = cont.addImageButton("white", "toggle", 14, () -> {
				lastColor = (byte)f;
				setConfigure(tile, (byte)f);
			}).size(18, 24).padBottom(-2.1f).group(group).get();
			button.getStyle().imageUpColor = colorArray[f];
			button.setChecked(entity.color == f);

			if(i%4 == 3){
				cont.row();
			}
		}

		table.add(cont);
	}

	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		TeleporterEntity entity = tile.entity();
		return !(source.block() instanceof MassMatrix) && entity.getBuffered(item) < capacity;
	}

	@Override
	public void handleItem(Item item, Tile tile, Tile source){
		TeleporterEntity entity = tile.entity();
		entity.addBuffered(item, 1);
	}

	//tries to send all items
	public void sendItems( Tile tile) {
		TeleporterEntity entity = tile.entity();

		Array<Tile> links = findLinks(tile);

		int fakePackets = 1;

		for(int item : entity.itemsBuffered){ if(item > 0) fakePackets++; }

		int[] initBuffer = new int[entity.itemsBuffered.length];
		for(int i = 0; i < entity.itemsBuffered.length; i++) initBuffer[i] = entity.itemsBuffered[i];

		if (links.size > 0) {
			if (!syncBlockState || Net.server() || !Net.active()) {
				Tile target = links.random();

				for (int i = 0; i < fakePackets; i++) {
					for (Item item : Item.getAllItems()) {
						if (entity.power >= (powerPerItem*(initBuffer[item.id] / fakePackets))) {

							target.entity.addItem(item, initBuffer[item.id] / fakePackets);
							entity.removeBuffered(item, initBuffer[item.id] / fakePackets);

							entity.power -= (powerPerItem * initBuffer[item.id] / fakePackets);
						}
					}
				}
			}
		}
	}

	@Override
	public TileEntity getEntity(){
		return new TeleporterEntity();
	}
	
	private Array<Tile> findLinks(Tile tile){
		TeleporterEntity entity = tile.entity();
		
		removal.clear();
		returns.clear();
		
		for(Tile other : teleporters[entity.color]){
			if(other != tile){
				if(other.block() instanceof MassMatrix){
					if(other.<TeleporterEntity>entity().color != entity.color){
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
			teleporters[entity.color].remove(remove);
		
		return returns;
	}

	public static class TeleporterEntity extends PowerEntity{
		public byte color = 0;
		public int[] itemsBuffered = new int[Item.getAllItems().size];

		public void addBuffered(Item item, int amount){ itemsBuffered[item.id] += amount; }

		public void removeBuffered(Item item, int amount){ itemsBuffered[item.id] -= amount; }

		public int getBuffered(Item item){
			return itemsBuffered[item.id];
		}

		@Override
		public void write(DataOutputStream stream) throws IOException{
			stream.writeByte(color);
		}
		
		@Override
		public void read(DataInputStream stream) throws IOException{
			color = stream.readByte();
		}
	}

}
