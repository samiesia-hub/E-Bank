
"use client";

import { useMemo, useState } from "react";
import {
  LayoutDashboard,
  Wallet,
  ArrowLeftRight,
  Users,
  CreditCard,
  Landmark,
  FileText,
  Headphones,
  Settings,
  LogOut,
  Search,
  Bell,
  ChevronDown,
  ChevronRight,
  Plus,
  Receipt,
  ShieldAlert,
  ArrowUpRight,
  ArrowDownLeft,
  Menu,
  X,
  Eye,
  CheckCircle2,
  Clock3,
  CalendarDays,
  LockKeyhole,
  Info,
} from "lucide-react";

import type { LucideIcon } from "lucide-react";

import {
  client,
  accounts,
  transactions,
  monthlyExpenses,
  loan,
  notifications,
  formatMAD,
} from "@/data/dashboardData";

// Navigation latérale
const navigation: {
  label: string;
  icon: LucideIcon;
}[] = [
  { label: "Tableau de bord", icon: LayoutDashboard },
  { label: "Mes comptes", icon: Wallet },
  { label: "Virements", icon: ArrowLeftRight },
  { label: "Bénéficiaires", icon: Users },
  { label: "Cartes", icon: CreditCard },
  { label: "Crédits", icon: Landmark },
  { label: "Relevés", icon: FileText },
  { label: "Support", icon: Headphones },
  { label: "Paramètres", icon: Settings },
];

// Boutons des actions rapides
const quickActions: {
  label: string;
  icon: LucideIcon;
  primary?: boolean;
}[] = [
  {
    label: "Nouveau virement",
    icon: ArrowLeftRight,
    primary: true,
  },
  {
    label: "Ajouter un bénéficiaire",
    icon: Plus,
  },
  {
    label: "Payer une facture",
    icon: Receipt,
  },
  {
    label: "Bloquer ma carte",
    icon: ShieldAlert,
  },
];

export default function ClientDashboard() {
  const [activePage, setActivePage] = useState("Tableau de bord");
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const [notificationOpen, setNotificationOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [modal, setModal] = useState<string | null>(null);
  const [readNotifications, setReadNotifications] = useState<number[]>([]);
  const [today] = useState(() =>
    new Intl.DateTimeFormat("fr-FR", {
        weekday: "long",
        day: "numeric",
        month: "long",
        year: "numeric",
      }).format(new Date())
  );

  // Filtrage des transactions
  const filteredTransactions = useMemo(() => {
    const query = search.trim().toLowerCase();

    if (!query) return transactions;

    return transactions.filter((transaction) =>
      [
        transaction.label,
        transaction.category,
        transaction.date,
        transaction.status,
      ].some((value) => value.toLowerCase().includes(query))
    );
  }, [search]);

  const unreadCount = notifications.filter(
    (notification) =>
      notification.unread &&
      !readNotifications.includes(notification.id)
  ).length;

  function navigateTo(page: string) {
    setActivePage(page);
    setSidebarOpen(false);
    setUserMenuOpen(false);
    setSearch("");
  }

  function markNotificationsRead() {
    setReadNotifications(notifications.map((item) => item.id));
  }

  function openAction(label: string) {
    setModal(label);
  }

  return (
    <div className="bankati-app">
      {/* BARRE SUPÉRIEURE */}
      <header className="topbar">
        <div className="topbar-brand">
          <button
            className="mobile-menu-button"
            onClick={() => setSidebarOpen(!sidebarOpen)}
            aria-label="Ouvrir le menu"
          >
            <Menu size={22} />
          </button>

          <div className="brand-logo">
            <Landmark size={27} strokeWidth={2} />
            <span>Bankati</span>
          </div>
        </div>

        <div className="topbar-search">
          <Search size={18} />

          <input
            type="search"
            placeholder="Rechercher une transaction..."
            aria-label="Rechercher une transaction"
            value={search}
            onChange={(event) => {
              setSearch(event.target.value);
              setActivePage("Tableau de bord");
            }}
          />
        </div>

        <div className="topbar-actions">
          <div className="dropdown-wrapper">
            <button
              className="notification-button"
              aria-label="Notifications"
              aria-expanded={notificationOpen}
              onClick={() => {
                setNotificationOpen(!notificationOpen);
                setUserMenuOpen(false);
              }}
            >
              <Bell size={21} />

              {unreadCount > 0 && (
                <span className="notification-dot" />
              )}
            </button>

            {notificationOpen && (
              <div className="top-dropdown notification-dropdown">
                <div className="dropdown-heading">
                  <strong>Notifications</strong>
                  <button
                    onClick={markNotificationsRead}
                    className="text-action"
                  >
                    Tout marquer comme lu
                  </button>
                </div>

                {notifications.map((item) => (
                  <div className="dropdown-notification" key={item.id}>
                    <strong>{item.title}</strong>
                    <p>{item.message}</p>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="topbar-divider" />

          <div className="dropdown-wrapper">
            <button
              className="profile-button"
              aria-expanded={userMenuOpen}
              onClick={() => {
                setUserMenuOpen(!userMenuOpen);
                setNotificationOpen(false);
              }}
            >
              <span className="avatar">{client.initials}</span>
              <span className="profile-name">
                {client.firstName} {client.lastName}
              </span>
              <ChevronDown size={16} />
            </button>

            {userMenuOpen && (
              <div className="top-dropdown user-dropdown">
                <button onClick={() => navigateTo("Paramètres")}>
                  <Settings size={17} />
                  Paramètres du compte
                </button>

                <button onClick={() => openAction("Déconnexion")}>
                  <LogOut size={17} />
                  Déconnexion
                </button>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* MENU LATÉRAL */}
      <aside className={`sidebar ${sidebarOpen ? "sidebar-open" : ""}`}>
        <div className="sidebar-content">
          <p className="sidebar-label">ESPACE PERSONNEL</p>

          <nav aria-label="Navigation principale">
            {navigation.map((item) => {
              const Icon = item.icon;

              return (
                <button
                  key={item.label}
                  className={`sidebar-link ${
                    activePage === item.label ? "active" : ""
                  }`}
                  onClick={() => navigateTo(item.label)}
                >
                  <Icon size={19} strokeWidth={1.9} />
                  <span>{item.label}</span>
                </button>
              );
            })}
          </nav>
        </div>

        <div className="sidebar-footer">
          <button
            className="sidebar-link"
            onClick={() => openAction("Déconnexion")}
          >
            <LogOut size={19} />
            <span>Déconnexion</span>
          </button>

          <div className="sidebar-security">
            <LockKeyhole size={14} />
            <span>Espace bancaire sécurisé</span>
          </div>
        </div>
      </aside>

      {/* CONTENU PRINCIPAL */}
      <div className="main-content">
        {activePage === "Tableau de bord" ? (
          <>
            {/* EN-TÊTE */}
            <div className="page-header">
              <div>
                <p className="page-eyebrow">ESPACE CLIENT</p>
                <h1>Bonjour, {client.firstName}</h1>
                <p className="page-subtitle">
                  Bienvenue dans votre espace bancaire personnel.
                </p>
              </div>

              <div className="login-info">
                <span className="current-date">
                  <CalendarDays size={16} />
                  {today}
                </span>
                <span>
                  Dernière connexion : {client.lastLogin}
                </span>
              </div>
            </div>

            {/* MES COMPTES */}
            <section className="dashboard-section">
              <div className="section-heading">
                <h2>Vue d’ensemble de mes comptes</h2>
                <button
                  className="text-link"
                  onClick={() => navigateTo("Mes comptes")}
                >
                  Tous mes comptes
                  <ChevronRight size={16} />
                </button>
              </div>

              <div className="accounts-grid">
                {accounts.map((account, index) => (
                  <article className="account-card" key={account.id}>
                    <div className="account-top">
                      <div className="account-icon">
                        {index === 0 ? (
                          <Wallet size={21} />
                        ) : index === 1 ? (
                          <Landmark size={21} />
                        ) : (
                          <LockKeyhole size={21} />
                        )}
                      </div>

                      <span className="account-type">MAD</span>
                    </div>

                    <h3>{account.name}</h3>
                    <p className="account-balance">
                      {formatMAD(account.balance)}
                    </p>

                    <div className="account-bottom">
                      <span className="rib">{account.rib}</span>

                      <button
                        className="account-details"
                        onClick={() =>
                          openAction(`Compte : ${account.name}`)
                        }
                      >
                        Voir détails
                        <ArrowUpRight size={15} />
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            </section>

            {/* ACTIONS RAPIDES */}
            <section className="dashboard-section">
              <div className="section-heading">
                <h2>Actions rapides</h2>
              </div>

              <div className="quick-actions">
                {quickActions.map((action) => {
                  const Icon = action.icon;

                  return (
                    <button
                      key={action.label}
                      className={`quick-action ${
                        action.primary ? "quick-action-primary" : ""
                      }`}
                      onClick={() => openAction(action.label)}
                    >
                      <Icon size={19} />
                      <span>{action.label}</span>
                    </button>
                  );
                })}
              </div>
            </section>

            {/* TRANSACTIONS ET COLONNE DROITE */}
            <section className="dashboard-main-grid">
              <article className="panel transactions-panel">
                <div className="panel-heading">
                  <div>
                    <h2>Dernières transactions</h2>
                    <p>Activité récente de vos comptes</p>
                  </div>

                  <button
                    className="text-link"
                    onClick={() => navigateTo("Mes comptes")}
                  >
                    Voir tout
                    <ChevronRight size={16} />
                  </button>
                </div>

                <div className="table-scroll">
                  <table className="transactions-table">
                    <thead>
                      <tr>
                        <th>Date</th>
                        <th>Libellé</th>
                        <th>Catégorie</th>
                        <th>Montant</th>
                        <th>Statut</th>
                      </tr>
                    </thead>

                    <tbody>
                      {filteredTransactions.map((transaction) => (
                        <tr key={transaction.id}>
                          <td className="date-cell">
                            {transaction.date}
                          </td>

                          <td>
                            <div className="transaction-label">
                              <span
                                className={`transaction-icon ${
                                  transaction.amount >= 0
                                    ? "incoming"
                                    : "outgoing"
                                }`}
                              >
                                {transaction.amount >= 0 ? (
                                  <ArrowDownLeft size={16} />
                                ) : (
                                  <ArrowUpRight size={16} />
                                )}
                              </span>

                              <span>{transaction.label}</span>
                            </div>
                          </td>

                          <td className="category-cell">
                            {transaction.category}
                          </td>

                          <td
                            className={`amount-cell ${
                              transaction.amount >= 0
                                ? "positive"
                                : "negative"
                            }`}
                          >
                            {transaction.amount >= 0 ? "+" : "−"}
                            {formatMAD(Math.abs(transaction.amount))}
                          </td>

                          <td>
                            <span
                              className={`status-badge ${
                                transaction.status === "Validé"
                                  ? "status-valid"
                                  : "status-pending"
                              }`}
                            >
                              {transaction.status === "Validé" ? (
                                <CheckCircle2 size={13} />
                              ) : (
                                <Clock3 size={13} />
                              )}

                              {transaction.status}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>

                  {filteredTransactions.length === 0 && (
                    <p className="empty-state">
                      Aucune transaction ne correspond à votre recherche.
                    </p>
                  )}
                </div>

                <div className="panel-footer">
                  <span>
                    {filteredTransactions.length} transaction(s) affichée(s)
                  </span>
                  <span>Montants en MAD</span>
                </div>
              </article>

              <div className="right-column">
                {/* CARTE BANCAIRE */}
                <article className="panel card-panel">
                  <div className="panel-heading">
                    <div>
                      <h2>Ma carte bancaire</h2>
                      <p>Carte principale</p>
                    </div>

                    <button
                      className="icon-link"
                      onClick={() => navigateTo("Cartes")}
                      aria-label="Gérer mes cartes"
                    >
                      <ChevronRight size={19} />
                    </button>
                  </div>

                  <div className="bank-card">
                    <div className="bank-card-top">
                      <strong>Bankati</strong>
                      <span>Débit</span>
                    </div>

                    <div className="card-chip">
                      <span />
                    </div>

                    <div className="card-number">
                      •••• &nbsp; •••• &nbsp; •••• &nbsp; 4829
                    </div>

                    <div className="bank-card-bottom">
                      <div>
                        <small>TITULAIRE</small>
                        <strong>KARIM BENALI</strong>
                      </div>

                      <div>
                        <small>EXPIRE FIN</small>
                        <strong>11/28</strong>
                      </div>
                    </div>
                  </div>

                  <button
                    className="secondary-action"
                    onClick={() => navigateTo("Cartes")}
                  >
                    Gérer ma carte
                    <ChevronRight size={16} />
                  </button>
                </article>

                {/* GRAPHIQUE */}
                <article className="panel expenses-panel">
                  <div className="panel-heading">
                    <div>
                      <h2>Dépenses du mois</h2>
                      <p>Évolution sur les 6 derniers mois</p>
                    </div>
                  </div>

                  <div className="chart-summary">
                    <strong>{formatMAD(6200)}</strong>
                    <span>Octobre 2026</span>
                  </div>

                  <div
                    className="bar-chart"
                    role="img"
                    aria-label="Comparaison des dépenses mensuelles de mai à octobre 2026"
                  >
                    <div className="chart-y-axis">
                      <span>6k</span>
                      <span>4k</span>
                      <span>2k</span>
                      <span>0</span>
                    </div>

                    <div className="chart-plot">
                      <div className="chart-gridlines">
                        <span />
                        <span />
                        <span />
                        <span />
                      </div>

                      <div className="chart-bars">
                        {monthlyExpenses.map((month) => (
                          <div className="chart-group" key={month.month}>
                            <div className="chart-pair">
                              <div
                                className="bar bar-previous"
                                style={{
                                  height: `${(month.previous / 7000) * 100}%`,
                                }}
                                title={`Période précédente : ${formatMAD(month.previous)}`}
                              />

                              <div
                                className="bar bar-current"
                                style={{
                                  height: `${(month.current / 7000) * 100}%`,
                                }}
                                title={`Dépenses : ${formatMAD(month.current)}`}
                              />
                            </div>

                            <span className="chart-month">
                              {month.month}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>

                  <div className="chart-legend">
                    <span>
                      <i className="legend-current" />
                      2026
                    </span>
                    <span>
                      <i className="legend-previous" />
                      2025
                    </span>
                  </div>
                </article>
              </div>
            </section>

            {/* CRÉDIT ET NOTIFICATIONS */}
            <section className="bottom-grid">
              <article className="panel loan-panel">
                <div className="panel-heading">
                  <div>
                    <h2>Crédit en cours</h2>
                    <p>{loan.name}</p>
                  </div>

                  <span className="loan-icon">
                    <Landmark size={21} />
                  </span>
                </div>

                <div className="loan-stats">
                  <div>
                    <span>Montant initial</span>
                    <strong>{formatMAD(loan.totalAmount)}</strong>
                  </div>

                  <div>
                    <span>Mensualité</span>
                    <strong>{formatMAD(loan.monthlyPayment)}</strong>
                  </div>
                </div>

                <div className="loan-progress-heading">
                  <span>Échéances remboursées</span>
                  <strong>
                    {loan.paidInstallments} / {loan.totalInstallments}
                  </strong>
                </div>

                <div
                  className="progress-track"
                  role="progressbar"
                  aria-valuenow={loan.paidInstallments}
                  aria-valuemin={0}
                  aria-valuemax={loan.totalInstallments}
                >
                  <div
                    className="progress-fill"
                    style={{
                      width: `${
                        (loan.paidInstallments / loan.totalInstallments) *
                        100
                      }%`,
                    }}
                  />
                </div>

                <div className="loan-footer">
                  <span>
                    <CalendarDays size={16} />
                    Prochaine échéance : {loan.nextPayment}
                  </span>

                  <button
                    className="text-link"
                    onClick={() => navigateTo("Crédits")}
                  >
                    Détails
                    <ChevronRight size={16} />
                  </button>
                </div>
              </article>

              <article className="panel alerts-panel">
                <div className="panel-heading">
                  <div>
                    <h2>Notifications</h2>
                    <p>Informations sur votre compte</p>
                  </div>

                  {unreadCount > 0 && (
                    <span className="alert-count">
                      {unreadCount} nouvelles
                    </span>
                  )}
                </div>

                <div className="alerts-list">
                  {notifications.map((item) => {
                    const isUnread =
                      item.unread &&
                      !readNotifications.includes(item.id);

                    return (
                      <div className="alert-item" key={item.id}>
                        <div className="alert-icon">
                          <Bell size={17} />
                        </div>

                        <div className="alert-content">
                          <div className="alert-title-row">
                            <strong>{item.title}</strong>
                            {isUnread && <span className="unread-dot" />}
                          </div>

                          <p>{item.message}</p>
                          <small>{item.date}</small>
                        </div>
                      </div>
                    );
                  })}
                </div>

                <button
                  className="alerts-footer-link"
                  onClick={() => {
                    setNotificationOpen(true);
                    markNotificationsRead();
                  }}
                >
                  Voir mes notifications
                  <ChevronRight size={16} />
                </button>
              </article>
            </section>

            <footer className="dashboard-footer">
              <span>© 2026 Bankati. Tous droits réservés.</span>
              <span>Votre sécurité est notre priorité.</span>
            </footer>
          </>
        ) : (
          /* Page temporaire pour les autres rubriques */
          <div className="placeholder-page">
            <div className="placeholder-icon">
              <Info size={28} />
            </div>

            <h1>{activePage}</h1>

            <p>
              Cet espace sera disponible après l’intégration
              des fonctionnalités bancaires correspondantes.
            </p>

            <button
              className="primary-button"
              onClick={() => navigateTo("Tableau de bord")}
            >
              Retour au tableau de bord
            </button>
          </div>
        )}
      </div>

      {/* FENÊTRE MODALE */}
      {modal && (
        <div
          className="modal-backdrop"
          onClick={() => setModal(null)}
        >
          <div
            className="modal-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="modal-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="modal-header">
              <h2 id="modal-title">{modal}</h2>

              <button
                className="modal-close"
                onClick={() => setModal(null)}
                aria-label="Fermer"
              >
                <X size={21} />
              </button>
            </div>

            <div className="modal-body">
              {modal.startsWith("Compte :") ? (
                <>
                  <Eye size={27} className="modal-info-icon" />

                  <p>
                    Les informations détaillées de ce compte
                    seront affichées ici après connexion au backend.
                  </p>
                </>
              ) : modal === "Bloquer ma carte" ? (
                <>
                  <ShieldAlert
                    size={27}
                    className="modal-warning-icon"
                  />

                  <p>
                    Le blocage d’une carte nécessite une confirmation
                    sécurisée. Aucune opération n’a été effectuée.
                  </p>
                </>
              ) : modal === "Déconnexion" ? (
                <p>
                  La déconnexion sera activée lors de
                  l’intégration de l’authentification.
                </p>
              ) : (
                <p>
                  Cette fonctionnalité sera connectée aux services
                  bancaires de Bankati dans une prochaine étape.
                </p>
              )}
            </div>

            <div className="modal-footer">
              <button
                className="secondary-button"
                onClick={() => setModal(null)}
              >
                Fermer
              </button>
            </div>
          </div>
        </div>
      )}

      {/* FOND MOBILE */}
      {sidebarOpen && (
        <button
          className="sidebar-overlay"
          aria-label="Fermer le menu"
          onClick={() => setSidebarOpen(false)}
        />
      )}
    </div>
  );
}
