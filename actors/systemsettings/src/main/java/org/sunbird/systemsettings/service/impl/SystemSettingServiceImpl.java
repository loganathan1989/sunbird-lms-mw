package org.sunbird.systemsettings.service.impl;

import java.io.IOException;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.systemsettings.dao.SystemSettingDao;
import org.sunbird.systemsettings.dao.impl.SystemSettingDaoImpl;
import org.sunbird.systemsettings.model.SystemSetting;
import org.sunbird.systemsettings.service.SystemSettingService;

/**
 * This class implements the methods to write and read the system settings table through
 * systemSettingDao
 *
 * @author Loganathan
 */
public class SystemSettingServiceImpl implements SystemSettingService {
  private SystemSettingDao systemSettingDao;

  public SystemSettingServiceImpl(CassandraOperation cassandraOperation) {
    this.systemSettingDao = new SystemSettingDaoImpl(cassandraOperation);
  }

  /**
   * This methods writes the setting to the System settings
   *
   * @param systemSetting instance of SystemSetting class has the values to be written
   *     (id,field,value)
   * @return returns the instance of Reponse class with 'id' of created record
   */
  @Override
  public Response setSetting(SystemSetting systemSetting) throws IOException {
    Response response = this.systemSettingDao.upsert(systemSetting);
    return response;
  }

  /**
   * This methods reads the setting from System settings by its id
   *
   * @param id id of the setting to be fetched from system settings
   * @return returns the instance of SystemSetting class with elements id,field,value
   */
  @Override
  public SystemSetting readSetting(String id) throws IOException {
    SystemSetting systemSetting = this.systemSettingDao.readById(id);
    return systemSetting;
  }

  /**
   * This methods reads all the settings from System settings
   *
   * @return returns the instance of Response class with settings elements list
   */
  @Override
  public Response readAllSettings() {
    Response response = this.systemSettingDao.readAll();
    return response;
  }
}