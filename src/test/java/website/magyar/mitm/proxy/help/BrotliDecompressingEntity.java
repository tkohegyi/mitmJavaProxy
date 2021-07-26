package website.magyar.mitm.proxy.help;

import org.apache.http.HttpEntity;
import org.apache.http.client.entity.DecompressingEntity;

public class BrotliDecompressingEntity extends DecompressingEntity {

    public BrotliDecompressingEntity(final HttpEntity entity) {
            super(entity, BrotliInputStreamFactory.getInstance());
        }

}
