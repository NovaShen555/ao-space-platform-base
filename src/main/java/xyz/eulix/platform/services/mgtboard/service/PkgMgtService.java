package xyz.eulix.platform.services.mgtboard.service;

import org.jboss.logging.Logger;
import xyz.eulix.platform.services.mgtboard.dto.*;
import xyz.eulix.platform.services.mgtboard.entity.PkgInfoEntity;
import xyz.eulix.platform.services.mgtboard.repository.PkgInfoEntityRepository;
import xyz.eulix.platform.services.support.service.ServiceError;
import xyz.eulix.platform.services.support.service.ServiceOperationException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

@ApplicationScoped
public class PkgMgtService {
    private static final Logger LOG = Logger.getLogger("app.log");

    @Inject
    PkgInfoEntityRepository pkgInfoEntityRepository;

    /**
     * 新增pkg版本
     *
     * @param packageReq pkg版本信息
     * @return pkg版本信息
     */
    @Transactional
    public PackageRes savePkgInfo(PackageReq packageReq) {
        // 校验版本是否存在
        PkgInfoEntity PkgInfoEntityOld = pkgInfoEntityRepository.findByAppNameAndTypeAndVersion(packageReq.getPkgName(),
                packageReq.getPkgType(), packageReq.getPkgVersion());
        if (PkgInfoEntityOld != null) {
            LOG.warnv("pkg version already exist, pkgName:{0}, pkgType:{1}, pkgVersion:{2}", packageReq.getPkgName(),
                    packageReq.getPkgType(), packageReq.getPkgVersion());
            throw new ServiceOperationException(ServiceError.PKG_VERSION_EXISTED);
        }
        PkgInfoEntity PkgInfoEntity = pkgInfoReqToEntity(packageReq);
        pkgInfoEntityRepository.persist(PkgInfoEntity);

        return pkgInfoEntityToRes(PkgInfoEntity);
    }


    /**
     * 更新pkg版本
     *
     * @param packageReq pkg版本信息
     * @return pkg版本信息
     */
    @Transactional
    public PackageRes updatePkginfo(PackageReq packageReq) {
        // 校验版本是否存在
        PkgInfoEntity PkgInfoEntityOld = pkgInfoEntityRepository.findByAppNameAndTypeAndVersion(packageReq.getPkgName(),
                packageReq.getPkgType(), packageReq.getPkgVersion());
        if (PkgInfoEntityOld == null) {
            LOG.warnv("pkg version does not exist, pkgName:{0}, pkgType:{1}, pkgVersion:{2}", packageReq.getPkgName(),
                    packageReq.getPkgType(), packageReq.getPkgVersion());
            throw new ServiceOperationException(ServiceError.PKG_VERSION_NOT_EXIST);
        }
        PkgInfoEntity PkgInfoEntity = pkgInfoReqToEntity(packageReq);
        pkgInfoEntityRepository.updateByAppNameAndTypeAndVersion(PkgInfoEntity);
        return pkgInfoEntityToRes(PkgInfoEntity);
    }

    /**
     * 删除pkg版本
     *  @param pkgName pkg名称
     * @param pkgType pkg类型
     * @param pkgVersion pkg版本号
     */
    @Transactional
    public void delPkginfo(String pkgName, String pkgType, String pkgVersion) {
        pkgInfoEntityRepository.deleteByAppNameAndTypeAndVersion(pkgName, pkgType, pkgVersion);
    }


    /**
     * 检查 App 版本
     * @param appName app name
     * @param appType app 类型
     * @param curBoxVersion 盒子版本
     * @param curAppVersion App版本
     * @return 检查结果
     */
    public PackageCheckRes checkAppInfo(String appName, String appType, String curAppVersion,String boxName,String boxType, String curBoxVersion) {
        PackageCheckRes packageCheckRes = new PackageCheckRes();

        // 查询当前 app 版本
        PkgInfoEntity curAppPkg = pkgInfoEntityRepository.findByAppNameAndTypeAndVersion(appName, appType, curAppVersion);
        if (curAppPkg == null){
            LOG.warnv("app version does not exist, appName:{0}, appType:{1}, appVersion:{2}", appName, appType, curAppVersion);
            return packageCheckRes;
        }
        // 查询最新 app、box 版本
        PkgInfoEntity latestBoxPkg = pkgInfoEntityRepository.findByAppNameAndTypeSortedByVersion(boxName, boxType);
        PkgInfoEntity latestAppPkg = pkgInfoEntityRepository.findByAppNameAndTypeSortedByVersion(appName, appType);

        // 判断是否需要更新
        if (curAppVersion.compareToIgnoreCase(latestAppPkg.getPkgVersion()) < 0) {
            LOG.infov(
                "app version need to update, appName:{0}, appType:{1}, from curVersion:{2} to newVersion:{3}",
                appName, appType, curAppVersion, latestAppPkg.getPkgVersion());
            packageCheckRes.setLatestAppPkg(pkgInfoEntityToRes(latestAppPkg));
            packageCheckRes.setNewVersionExist(true);

            // 判断 app 新版本与当前 box 版本兼容性
            if (curBoxVersion.compareToIgnoreCase(latestAppPkg.getMinCompatibleBoxVersion())  < 0) {
                // 当前盒子的版本比最新 app 兼容 box 版本低
                packageCheckRes.setLatestBoxPkg(pkgInfoEntityToRes(latestBoxPkg));
                packageCheckRes.setIsBoxNeedUpdate(true);
            }
        }
        return packageCheckRes;
    }
    /**
     * 检查 Box 版本
     * @param boxName Box name
     * @param curBoxVersion 盒子版本
     * @param curAppVersion App版本
     * @return 检查结果
     */
    public PackageCheckRes checkBoxInfo(String appName, String appType, String curAppVersion,String boxName,String boxType, String curBoxVersion) {
        PackageCheckRes packageCheckRes = new PackageCheckRes();

        // 查询当前 Box 版本
        PkgInfoEntity curBoxPkg = pkgInfoEntityRepository.findByAppNameAndTypeAndVersion(boxName, boxType, curBoxVersion);
        if (curBoxPkg == null){
            LOG.warnv("box version does not exist, boxName:{0}, boxType:{1}, boxVersion:{2}", boxName, boxType, curBoxVersion);
            return packageCheckRes;
        }
        // 查询最新 box 版本
        PkgInfoEntity latestBoxPkg = pkgInfoEntityRepository.findByAppNameAndTypeSortedByVersion(boxName, boxType);
        // 查询最新 app 版本
        PkgInfoEntity latestAppPkg = pkgInfoEntityRepository.findByAppNameAndTypeSortedByVersion(appName, appType);
        String latestMinAppVersion;
        if("ios".equals(appType)){
            latestMinAppVersion = latestBoxPkg.getMinCompatibleIOSVersion();
        }else {
            latestMinAppVersion = latestBoxPkg.getMinCompatibleAndroidVersion();
        }

        // 判断是否需要更新
        if (curAppVersion.compareToIgnoreCase(latestBoxPkg.getPkgVersion()) < 0) {
            LOG.infov(
                "box version need to update, boxName:{0}, boxType:{1}, from curVersion:{2} to newVersion:{3}",
                boxName, "box", curBoxVersion, latestBoxPkg.getPkgVersion());
            packageCheckRes.setLatestBoxPkg(pkgInfoEntityToRes(latestBoxPkg));
            packageCheckRes.setNewVersionExist(true);
            // 判断 box 新版本与当前 app 版本兼容性
            if (curAppVersion.compareToIgnoreCase(latestMinAppVersion)  < 0) {
                // 当前 app 的版本比最新 box 兼容 app 版本低
                packageCheckRes.setLatestAppPkg(pkgInfoEntityToRes(latestAppPkg));
                packageCheckRes.setIsAppNeedUpdate(true);
            }
        }

        return packageCheckRes;
    }

    /**
     * 强制升级 &兼容性 检测
     *
     * @param appPkgName app名称
     * @param appPkgType app类型
     * @param curAppVersion app当前版本
     * @param boxPkgName box名称
     * @param boxPkgType box类型
     * @param curBoxVersion box当前版本
     * @return 是否需要强制更新
     */
    public CompatibleCheckRes compatibleCheck(String appPkgName, String appPkgType, String curAppVersion, String boxPkgName,
                                              String boxPkgType, String curBoxVersion) {
        CompatibleCheckRes compatibleCheckRes = CompatibleCheckRes.of();
        // 0.校验当前版本是否存在
        PkgInfoEntity curAppPkg = pkgInfoEntityRepository.findByAppNameAndTypeAndVersion(appPkgName, appPkgType, curAppVersion);
        if (curAppPkg == null){
            LOG.warnv("app version does not exist, appName:{0}, appType:{1}, appVersion:{2}", appPkgName, appPkgType, curAppVersion);
            return compatibleCheckRes;
        }
        PkgInfoEntity curBoxPkg = pkgInfoEntityRepository.findByAppNameAndTypeAndVersion(boxPkgName, boxPkgType, curBoxVersion);
        if (curBoxPkg == null){
            LOG.warnv("box version does not exist, boxName:{0}, boxType:{1}, boxVersion:{2}", boxPkgName, boxPkgType, curBoxVersion);
            return compatibleCheckRes;
        }

        PkgInfoEntity targetAppPkgInfo = curAppPkg;     // 目标app版本=当前app版本
        PkgInfoEntity targetBoxPkgInfo = curBoxPkg;     // 目标box版本=当前box版本
        // 查询最新 app 版本
        PkgInfoEntity latestAppPkg = pkgInfoEntityRepository.findByAppNameAndTypeSortedByVersion(appPkgName, appPkgType);
        // 查询最新 box 版本
        PkgInfoEntity latestBoxPkg = pkgInfoEntityRepository.findByAppNameAndTypeSortedByVersion(boxPkgName, boxPkgType);
        // 1.检查当前app版本是否需要强制升级
        if (curAppPkg.getIsForceUpdate()) {
            if (curAppVersion.compareToIgnoreCase(latestAppPkg.getPkgVersion()) >= 0) {
                LOG.errorv("latest app version does not exist, appName:{0}, appType:{1}, appVersion:{2}", appPkgName, appPkgType, curAppVersion);
                throw new ServiceOperationException(ServiceError.LATEST_APP_VERSION_NOT_EXIST);
            }
            targetAppPkgInfo = latestAppPkg;    // 目标app版本=最新app版本
            compatibleCheckRes.setIsAppForceUpdate(true);
            compatibleCheckRes.setLastestAppPkg(pkgInfoEntityToRes(latestAppPkg));
            LOG.infov("app version needs to force upgrade, appName:{0}, appType:{1}, appVersion:{2}", appPkgName, appPkgType, curAppVersion);
        }
        // 2.检查当前box版本是否需要强制升级
        if (curBoxPkg.getIsForceUpdate()) {
            if (curBoxVersion.compareToIgnoreCase(latestBoxPkg.getPkgVersion()) >= 0) {
                LOG.errorv("latest box version does not exist, boxName:{0}, boxType:{1}, boxVersion:{2}", boxPkgName, boxPkgType, curBoxVersion);
                throw new ServiceOperationException(ServiceError.LATEST_BOX_VERSION_NOT_EXIST);
            }
            targetBoxPkgInfo = latestBoxPkg;    // 目标box版本=最新box版本
            compatibleCheckRes.setIsBoxForceUpdate(true);
            compatibleCheckRes.setLastestBoxPkg(pkgInfoEntityToRes(latestBoxPkg));
            LOG.infov("box version needs to force upgrade, boxName:{0}, boxType:{1}, boxVersion:{2}", boxPkgName, boxPkgType, curBoxVersion);
        }
        // 3.判断目标app版本与目标box版本是否兼容
        isCompatible(targetAppPkgInfo, latestAppPkg, targetBoxPkgInfo, latestBoxPkg, compatibleCheckRes);
        return compatibleCheckRes;
    }

    /**
     * 判断app版本与box版本是否兼容
     *
     * @param targetAppPkg app版本
     * @param latestAppPkg 最新box版本
     * @param targetBoxPkg box版本
     * @param latestBoxPkg 最新box版本
     * @return 是否兼容
     */
    private void isCompatible(PkgInfoEntity targetAppPkg, PkgInfoEntity latestAppPkg, PkgInfoEntity targetBoxPkg,
                                 PkgInfoEntity latestBoxPkg, CompatibleCheckRes compatibleCheckRes) {
        if (compatibleCheckRes.getIsAppForceUpdate() && compatibleCheckRes.getIsBoxForceUpdate()) {
            return;
        }
        // 1.检查目标app版本是否小于“目标box所兼容的最小app版本”
        String minCompatibleAppVersion;
        switch (PkgTypeEnum.fromValue(targetAppPkg.getPkgType())) {
            case ANDROID:
                minCompatibleAppVersion = targetBoxPkg.getMinCompatibleAndroidVersion();
                break;
            case IOS:
                minCompatibleAppVersion = targetBoxPkg.getMinCompatibleIOSVersion();
                break;
            default:
                throw new UnsupportedOperationException();
        }
        if (targetAppPkg.getPkgVersion().compareToIgnoreCase(minCompatibleAppVersion) < 0) {
            // 目标app版本比“目标box所兼容的最小app版本”低
            compatibleCheckRes.setIsAppForceUpdate(true);
            compatibleCheckRes.setLastestAppPkg(pkgInfoEntityToRes(latestAppPkg));
            LOG.infov("app version needs to force upgrade, appName:{0}, appType:{1}, appVersion:{2}", targetAppPkg.getPkgName(),
                    targetAppPkg.getPkgType(), targetAppPkg.getPkgVersion());

            //目标app版本=最新app版本,递归调用
            targetAppPkg = latestAppPkg;
            isCompatible(targetAppPkg, latestAppPkg, targetBoxPkg, latestBoxPkg, compatibleCheckRes);
        }

        // 2.检查目标box版本是否小于“目标app所兼容的最小box版本”
        String minCompatibleBoxVersion = targetAppPkg.getMinCompatibleBoxVersion();
        if (targetBoxPkg.getPkgVersion().compareToIgnoreCase(minCompatibleBoxVersion) < 0) {
            // 目标box的版本比“目标app所兼容的最小box版本”低
            compatibleCheckRes.setIsBoxForceUpdate(true);
            compatibleCheckRes.setLastestBoxPkg(pkgInfoEntityToRes(latestBoxPkg));
            LOG.infov("box version needs to force upgrade, boxName:{0}, boxType:{1}, boxVersion:{2}", targetBoxPkg.getPkgName(),
                    targetBoxPkg.getPkgType(), targetBoxPkg.getPkgVersion());

            //目标box版本=最新box版本,递归调用
            targetBoxPkg = latestBoxPkg;
            isCompatible(targetAppPkg, latestAppPkg, targetBoxPkg, latestBoxPkg, compatibleCheckRes);
        }
    }

    private PackageRes pkgInfoEntityToRes(PkgInfoEntity PkgInfoEntity) {
        return PackageRes.of(PkgInfoEntity.getPkgName(),
                PkgInfoEntity.getPkgType(),
                PkgInfoEntity.getPkgVersion(),
                PkgInfoEntity.getPkgSize(),
                PkgInfoEntity.getDownloadUrl(),
                PkgInfoEntity.getUpdateDesc(),
                PkgInfoEntity.getMd5(),
                PkgInfoEntity.getIsForceUpdate(),
                PkgInfoEntity.getMinCompatibleAndroidVersion(),
                PkgInfoEntity.getMinCompatibleIOSVersion(),
                PkgInfoEntity.getMinCompatibleBoxVersion());
    }

    private PkgInfoEntity pkgInfoReqToEntity(PackageReq packageReq) {
        return PkgInfoEntity.of(packageReq.getPkgName(),
                packageReq.getPkgType(),
                packageReq.getPkgVersion(),
                packageReq.getPkgSize(),
                packageReq.getUpdateDesc(),
                packageReq.getIsForceUpdate(),
                packageReq.getDownloadUrl(),
                packageReq.getMd5(),
                packageReq.getMinAndroidVersion(),
                packageReq.getMinIOSVersion(),
                packageReq.getMinBoxVersion(), null);
    }

  public PackageRes getBoxLatestVersion(String boxName, String boxType) {
      return pkgInfoEntityToRes(pkgInfoEntityRepository.findByAppNameAndTypeSortedByVersion(boxName, boxType));
  }
}
