import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/application/auth_session_controller.dart';
import '../../features/auth/application/auth_session_state.dart';
import '../../features/auth/presentation/screens/forgot_password_screen.dart';
import '../../features/auth/presentation/screens/login_screen.dart';
import '../../features/auth/presentation/screens/password_reset_confirm_screen.dart';
import '../../features/auth/presentation/screens/register_screen.dart';
import '../../features/bootstrap/presentation/screens/splash_screen.dart';
import '../../features/commerce/presentation/screens/category_list_screen.dart';
import '../../features/commerce/presentation/screens/home_screen.dart';
import '../../features/commerce/presentation/screens/product_detail_screen.dart';
import '../../features/commerce/presentation/screens/product_list_screen.dart';
import '../../features/commerce/presentation/screens/search_screen.dart';
import '../../features/account/presentation/screens/notifications_screen.dart';
import '../../features/account/presentation/screens/orders_screen.dart';
import '../../features/account/presentation/screens/profile_screen.dart';
import '../../features/account/presentation/screens/settings_screen.dart';
import '../../features/account/presentation/screens/support_screen.dart';
import '../../features/account/presentation/screens/legal_document_screen.dart';
import '../../features/onboarding/presentation/screens/onboarding_screen.dart';
import '../../features/shopping/presentation/screens/addresses_screen.dart';
import '../../features/shopping/presentation/screens/cart_screen.dart';
import '../../features/shopping/presentation/screens/checkout_screen.dart';
import '../../features/shopping/presentation/screens/order_detail_screen.dart';
import '../../features/wishlist/presentation/screens/wishlist_screen.dart';
import 'app_routes.dart';

const _guestOnlyPaths = <String>{
  AppRoutes.onboardingPath,
  AppRoutes.loginPath,
  AppRoutes.registerPath,
  AppRoutes.forgotPasswordPath,
  AppRoutes.passwordResetConfirmPath,
};

class RouterRefreshNotifier extends ChangeNotifier {
  void refresh() => notifyListeners();
}

final routerRefreshNotifierProvider = Provider<RouterRefreshNotifier>((ref) {
  final notifier = RouterRefreshNotifier();
  ref.listen<AsyncValue<AuthSessionState>>(
    authSessionControllerProvider,
    (
      AsyncValue<AuthSessionState>? previous,
      AsyncValue<AuthSessionState> next,
    ) => notifier.refresh(),
  );
  ref.onDispose(notifier.dispose);
  return notifier;
});

bool _requiresAuthentication(String location) {
  const privatePrefixes = <String>[
    '/cart',
    '/checkout',
    '/addresses',
    '/orders',
    '/wishlist',
    '/profile',
    '/settings',
    '/notifications',
    '/support',
  ];
  return privatePrefixes.any(location.startsWith);
}

final appRouterProvider = Provider<GoRouter>((ref) {
  final authSession = ref.watch(authSessionControllerProvider);
  final routerRefreshNotifier = ref.watch(routerRefreshNotifierProvider);

  final router = GoRouter(
    initialLocation: AppRoutes.splashPath,
    refreshListenable: routerRefreshNotifier,
    redirect: (BuildContext context, GoRouterState state) {
      final location = state.matchedLocation;
      final isAuthenticated = authSession.valueOrNull?.isAuthenticated ?? false;

      if (authSession.isLoading) {
        return location == AppRoutes.splashPath ? null : AppRoutes.splashPath;
      }

      if (location == '/') {
        return AppRoutes.splashPath;
      }

      if (location == AppRoutes.splashPath) {
        return isAuthenticated ? AppRoutes.homePath : AppRoutes.onboardingPath;
      }

      if (!isAuthenticated && _requiresAuthentication(location)) {
        return AppRoutes.loginPath;
      }

      if (isAuthenticated && _guestOnlyPaths.contains(location)) {
        return AppRoutes.homePath;
      }

      return null;
    },
    routes: [
      GoRoute(
        path: '/',
        builder: (BuildContext context, GoRouterState state) =>
            const SplashScreen(),
      ),
      GoRoute(
        path: AppRoutes.splashPath,
        name: AppRoutes.splashName,
        builder: (BuildContext context, GoRouterState state) =>
            const SplashScreen(),
      ),
      GoRoute(
        path: AppRoutes.onboardingPath,
        name: AppRoutes.onboardingName,
        builder: (BuildContext context, GoRouterState state) =>
            const OnboardingScreen(),
      ),
      GoRoute(
        path: AppRoutes.loginPath,
        name: AppRoutes.loginName,
        builder: (BuildContext context, GoRouterState state) =>
            const LoginScreen(),
      ),
      GoRoute(
        path: AppRoutes.registerPath,
        name: AppRoutes.registerName,
        builder: (BuildContext context, GoRouterState state) =>
            const RegisterScreen(),
      ),
      GoRoute(
        path: AppRoutes.forgotPasswordPath,
        name: AppRoutes.forgotPasswordName,
        builder: (BuildContext context, GoRouterState state) =>
            const ForgotPasswordScreen(),
      ),
      GoRoute(
        path: AppRoutes.passwordResetConfirmPath,
        name: AppRoutes.passwordResetConfirmName,
        builder: (BuildContext context, GoRouterState state) =>
            PasswordResetConfirmScreen(
              initialToken: state.uri.queryParameters['token'] ?? '',
            ),
      ),
      GoRoute(
        path: AppRoutes.homePath,
        name: AppRoutes.homeName,
        builder: (BuildContext context, GoRouterState state) =>
            const HomeScreen(),
      ),
      GoRoute(
        path: AppRoutes.categoriesPath,
        name: AppRoutes.categoriesName,
        builder: (BuildContext context, GoRouterState state) =>
            const CategoryListScreen(),
      ),
      GoRoute(
        path: AppRoutes.productsPath,
        name: AppRoutes.productsName,
        builder: (BuildContext context, GoRouterState state) =>
            ProductListScreen(
              initialQuery: state.uri.queryParameters['q'],
              initialCategoryId: state.uri.queryParameters['categoryId'],
              initialCategoryName: state.uri.queryParameters['categoryName'],
            ),
      ),
      GoRoute(
        path: AppRoutes.productDetailPath,
        name: AppRoutes.productDetailName,
        builder: (BuildContext context, GoRouterState state) =>
            ProductDetailScreen(
              productId: state.pathParameters['productId'] ?? '',
            ),
      ),
      GoRoute(
        path: AppRoutes.searchPath,
        name: AppRoutes.searchName,
        builder: (BuildContext context, GoRouterState state) =>
            const SearchScreen(),
      ),
      GoRoute(
        path: AppRoutes.cartPath,
        name: AppRoutes.cartName,
        builder: (BuildContext context, GoRouterState state) =>
            const CartScreen(),
      ),
      GoRoute(
        path: AppRoutes.checkoutPath,
        name: AppRoutes.checkoutName,
        builder: (BuildContext context, GoRouterState state) =>
            const CheckoutScreen(),
      ),
      GoRoute(
        path: AppRoutes.addressesPath,
        name: AppRoutes.addressesName,
        builder: (BuildContext context, GoRouterState state) => AddressesScreen(
          selectionMode: state.uri.queryParameters['select'] == '1',
        ),
      ),
      GoRoute(
        path: AppRoutes.ordersPath,
        name: AppRoutes.ordersName,
        builder: (BuildContext context, GoRouterState state) =>
            const OrdersScreen(),
      ),
      GoRoute(
        path: AppRoutes.orderDetailPath,
        name: AppRoutes.orderDetailName,
        builder: (BuildContext context, GoRouterState state) =>
            OrderDetailScreen(orderId: state.pathParameters['orderId'] ?? ''),
      ),
      GoRoute(
        path: AppRoutes.wishlistPath,
        name: AppRoutes.wishlistName,
        builder: (BuildContext context, GoRouterState state) =>
            const WishlistScreen(),
      ),
      GoRoute(
        path: AppRoutes.profilePath,
        name: AppRoutes.profileName,
        builder: (BuildContext context, GoRouterState state) =>
            const ProfileScreen(),
      ),
      GoRoute(
        path: AppRoutes.settingsPath,
        name: AppRoutes.settingsName,
        builder: (BuildContext context, GoRouterState state) =>
            const SettingsScreen(),
      ),
      GoRoute(
        path: AppRoutes.notificationsPath,
        name: AppRoutes.notificationsName,
        builder: (BuildContext context, GoRouterState state) =>
            const NotificationsScreen(),
      ),
      GoRoute(
        path: AppRoutes.supportPath,
        name: AppRoutes.supportName,
        builder: (BuildContext context, GoRouterState state) =>
            const SupportScreen(),
      ),
      GoRoute(
        path: AppRoutes.legalDocumentPath,
        name: AppRoutes.legalDocumentName,
        builder: (BuildContext context, GoRouterState state) =>
            LegalDocumentScreen(topicId: state.pathParameters['topicId'] ?? ''),
      ),
    ],
    errorBuilder: (BuildContext context, GoRouterState state) =>
        Scaffold(body: Center(child: Text('Route not found: ${state.uri}'))),
  );

  ref.onDispose(router.dispose);
  return router;
});
