package cn.dlb.bim.ifc.stream.serializers;

import java.io.IOException;

import org.springframework.data.util.CloseableIterator;

import cn.dlb.bim.database.DatabaseException;
import cn.dlb.bim.ifc.stream.VirtualObject;
import cn.dlb.bim.ifc.stream.query.QueryException;

public class SimpleObjectProvider implements ObjectProvider {
	
	private CloseableIterator<VirtualObject> iterator;
	
	public SimpleObjectProvider(CloseableIterator<VirtualObject> iterator) {
		this.iterator = iterator;
	}

	@Override
	public VirtualObject next() throws DatabaseException {
		if (iterator.hasNext()) {
			return iterator.next();
		} else {
			return null;
		}
	}

	@Override
	public ObjectProvider copy() throws IOException, QueryException {
		return null;
	}

}
