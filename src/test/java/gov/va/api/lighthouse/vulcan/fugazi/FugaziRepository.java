package gov.va.api.lighthouse.vulcan.fugazi;

import gov.va.api.health.autoconfig.logging.Loggable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

@Loggable
public interface FugaziRepository
    extends CrudRepository<FugaziEntity, String>, JpaSpecificationExecutor<FugaziEntity> {}
