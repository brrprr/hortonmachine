/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.dbs.compat;

/**
 * The available databases.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public enum EDb {
    SQLITE(0, ".sqlite", "sqlite", "org.hortonmachine.dbs.spatialite.hm.SqliteDb", false,
            "org.hortonmachine.dbs.spatialite.SpatialiteSqlTemplates", "jdbc:sqlite:", false, false, false, true), //
    SPATIALITE(1, ".sqlite", "sqlite", "org.hortonmachine.dbs.spatialite.hm.SpatialiteDb", true,
            "org.hortonmachine.dbs.spatialite.SpatialiteSqlTemplates", "jdbc:sqlite:", false, false, false, true), //
    H2(2, "", "mv.db", "org.hortonmachine.dbs.h2gis.H2Db", false, "org.hortonmachine.dbs.h2gis.H2GisSqlTemplates", "jdbc:h2:", true,
            true, false, true), //
    H2GIS(3, "", "mv.db", "org.hortonmachine.dbs.h2gis.H2GisDb", true, "org.hortonmachine.dbs.h2gis.H2GisSqlTemplates", "jdbc:h2:",
            true, true, false, true), //
    SPATIALITE4ANDROID(4, ".sqlite", "sqlite", "org.hortonmachine.dbs.spatialite.android.GPSpatialiteDb", true,
            "org.hortonmachine.dbs.spatialite.SpatialiteSqlTemplates", "", false, false, true, false); //

    private int _code;
    private String _extensionOnCreation;
    private String _extension;
    private String _dbClassName;
    private boolean _isSpatial;
    private String _sqlTemplatesClassName;
    private String _jdbcPrefix;
    private boolean _supportsPwd;
    private boolean _supportsServerMode;
    private boolean _supportsMobile;
    private boolean _supportsDesktop;

    private EDb( int code, String extensionOnCreation, String extension, String dbClassName, boolean isSpatial,
            String sqlTemplatesClassName, String jdbcPrefix, boolean supportsPwd, boolean supportsServerMode,
            boolean supportsMobile, boolean supportsDesktop ) {
        this._code = code;
        this._extensionOnCreation = extensionOnCreation;
        this._extension = extension;
        this._dbClassName = dbClassName;
        this._isSpatial = isSpatial;
        this._sqlTemplatesClassName = sqlTemplatesClassName;
        this._jdbcPrefix = jdbcPrefix;
        this._supportsPwd = supportsPwd;
        this._supportsServerMode = supportsServerMode;
        this._supportsMobile = supportsMobile;
        this._supportsDesktop = supportsDesktop;
    }

    public static EDb[] getSpatialTypesDesktop() {
        return new EDb[]{SPATIALITE, H2GIS};
    }

    public static EDb[] getSpatialTypesDesktopMobile() {
        return new EDb[]{SPATIALITE4ANDROID};
    }

    public int getCode() {
        return _code;
    }

    public String getJdbcPrefix() {
        return _jdbcPrefix;
    }

    public boolean supportsPwd() {
        return _supportsPwd;
    }

    public boolean supportsServerMode() {
        return _supportsServerMode;
    }

    public boolean supportsMobile() {
        return _supportsMobile;
    }

    public boolean supportsDesktop() {
        return _supportsDesktop;
    }

    public static EDb forCode( int code ) {
        for( EDb dbType : values() ) {
            if (dbType.getCode() == code) {
                return dbType;
            }
        }
        throw new IllegalArgumentException("No db type for code: " + code);
    }

    public String getExtensionOnCreation() {
        return _extensionOnCreation;
    }

    public String getExtension() {
        return _extension;
    }

    public boolean isSpatial() {
        return _isSpatial;
    }

    /**
     * Get a new instance of the spatial database version.
     * 
     * @return the spatial instance if the db supports it.
     * @throws Exception
     */
    public ASpatialDb getSpatialDb() throws Exception {
        if (_isSpatial) {
            Class< ? > forName = Class.forName(_dbClassName);
            Object newInstance = forName.newInstance();
            if (newInstance instanceof ASpatialDb) {
                return (ASpatialDb) newInstance;
            }
        }
        throw new IllegalArgumentException("Database type is not spatial");
    }

    /**
     * Get a new instance of the database.
     * 
     * @return the database instance.
     * @throws Exception
     */
    public ADb getDb() throws Exception {
        Class< ? > forName = Class.forName(_dbClassName);
        Object newInstance = forName.newInstance();
        return (ADb) newInstance;
    }

    /**
     * Get a new instance of the sql templates.
     * 
     * @return the sql templates instance.
     * @throws Exception
     */
    public ASqlTemplates getSqlTemplates() throws Exception {
        Class< ? > forName = Class.forName(_sqlTemplatesClassName);
        Object newInstance = forName.newInstance();
        return (ASqlTemplates) newInstance;
    }

}
