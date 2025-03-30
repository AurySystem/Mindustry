package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.style.TextureRegionDrawable;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Tmp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Debugspawner extends Block{
	protected final int timerDump = timers++;
	protected int capacity= 30;
	private static Item lastSpawn = Item.iron;
	
	public Debugspawner(String name) {
		super(name);
		update = true;
		solid = true;
	}

	@Override
	public void update(Tile tile){
		SpawnerEntity entity = tile.entity();
		
		if(entity.timer.get(timerDump, 5) && entity.hasItem(entity.spawnItem)){
			tryDump(tile, -1, entity.spawnItem);
		}
		
		if(entity.getItem(entity.spawnItem) <= capacity){
			offloadNear(tile, entity.spawnItem);
		}
	}

	@Override
	public void draw(Tile tile){
		super.draw(tile);
		
		SpawnerEntity entity = tile.entity();
		
		TextureRegion region = entity.spawnItem.region;
		Tmp.tr1.setRegion(region, 4, 4, 1, 1);
		
		Draw.rect(Tmp.tr1, tile.worldx(), tile.worldy(), 4f, 4f);
	}

	@Override
	public void placed(Tile tile){
		tile.<SpawnerEntity>entity().spawnItem = lastSpawn;
		setConfigure(tile, (byte)lastSpawn.id);
	}
	
	@Override
	public void configure(Tile tile, byte data) {
		SpawnerEntity entity = tile.entity();
		if(entity != null){
			entity.spawnItem = Item.getByID(data);
		}
	}

	@Override
	public boolean isConfigurable(Tile tile){
		return true;
	}
	
	@Override
	public void buildTable(Tile tile, Table table){
		SpawnerEntity entity = tile.entity();

		Array<Item> items = Item.getAllItems();

		ButtonGroup<ImageButton> group = new ButtonGroup<>();
		Table cont = new Table();
		cont.margin(4);
		cont.marginBottom(5);

		cont.add().colspan(4).height(105f);
		cont.row();

		for(int i = 0; i < items.size; i ++){
			final int f = i;
			ImageButton button = cont.addImageButton("white", "toggle", 24, () -> {
				lastSpawn = items.get(f);
				entity.spawnItem = items.get(f);
				setConfigure(tile, (byte)f);
                for(Item removeitem : Item.getAllItems()){
                	int removeamount = entity.getItem(removeitem);                	
                	entity.removeItem(removeitem,removeamount);
                }
			}).size(38, 42).padBottom(-5.1f).group(group).get();
			button.getStyle().imageUp = new TextureRegionDrawable(new TextureRegion(items.get(i).region));
			button.setChecked(entity.spawnItem.id == f);

			if(i%6 == 5){
				cont.row();
			}
		}

		table.add(cont);
	}
	
	@Override
	public TileEntity getEntity(){
		return new SpawnerEntity();
	}

	public static class SpawnerEntity extends TileEntity{
		public Item spawnItem = Item.iron;
		
		@Override
		public void write(DataOutputStream stream) throws IOException{
			stream.writeByte(spawnItem.id);
		}
		
		@Override
		public void read(DataInputStream stream) throws IOException{
			spawnItem = Item.getAllItems().get(stream.readByte());
		}
	}
}
